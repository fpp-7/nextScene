import os
import json
import time
import argparse
import logging
import backoff
import httpx
import pandas as pd

from pathlib import Path
from typing import Dict, Any, List, Optional

from src.config import DATA_PROCESSED_PATH, DATA_RAW_PATH, TMDB_API_KEY, TMDB_BASE_URL

# --- Setup Logging ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# --- Constants ---
CHECKPOINT_FILE = DATA_PROCESSED_PATH / "tmdb_checkpoint.json"
TMDB_METADATA_FILE = DATA_PROCESSED_PATH / "tmdb_metadata.parquet"
LINKS_FILE = DATA_RAW_PATH / "ml-latest-small" / "links.csv"

RATE_LIMIT_DELAY = 10 / 35  # 35 requests per 10 seconds
MAX_RETRIES = 5
BACKOFF_FACTOR = 2  # Exponential backoff base

# --- Helper Functions ---
def save_checkpoint(processed_ids: List[int]):
    """Saves the list of processed movie IDs to a checkpoint file."""
    try:
        with open(CHECKPOINT_FILE, 'w') as f:
            json.dump(list(processed_ids), f)
        logger.debug(f"Checkpoint saved with {len(processed_ids)} IDs.")
    except IOError as e:
        logger.error(f"Failed to save checkpoint: {e}")

def load_checkpoint() -> List[int]:
    """Loads processed movie IDs from a checkpoint file."""
    if CHECKPOINT_FILE.exists():
        try:
            with open(CHECKPOINT_FILE, 'r') as f:
                processed_ids = json.load(f)
            logger.info(f"Loaded {len(processed_ids)} IDs from checkpoint.")
            return processed_ids
        except json.JSONDecodeError as e:
            logger.warning(f"Error decoding checkpoint file, starting fresh. {e}")
            return []
        except IOError as e:
            logger.error(f"Failed to load checkpoint, starting fresh. {e}")
            return []
    return []

def format_name(name: str) -> str:
    """Formats a name string to lowercase with underscores."""
    return name.lower().replace(' ', '_')

def extract_tmdb_data(tmdb_id: int, response_json: Dict[str, Any]) -> Dict[str, Any]:
    """Extracts relevant data from the TMDB API response."""
    keywords = " ".join([format_name(kw['name']) for kw in response_json.get('keywords', {}).get('keywords', [])])

    directors = [
        format_name(crew['name'])
        for crew in response_json.get('credits', {}).get('crew', [])
        if crew.get('job') == 'Director'
    ][:2]  # Max 2 directors
    director_str = " ".join(directors)

    cast = [
        format_name(actor['name'])
        for actor in response_json.get('credits', {}).get('cast', [])
    ][:5]  # Top 5 cast members
    cast_str = " ".join(cast)

    return {
        'tmdbId': tmdb_id,
        'keywords': keywords,
        'director': director_str,
        'cast': cast_str
    }

@backoff.on_exception(
    backoff.expo,
    (httpx.RequestError, httpx.HTTPStatusError),
    max_tries=MAX_RETRIES,
    factor=BACKOFF_FACTOR,
    giveup=lambda e: e.response and e.response.status_code not in [429, 500, 502, 503, 504]
)
async def fetch_movie_metadata(client: httpx.AsyncClient, tmdb_id: int) -> Optional[Dict[str, Any]]:
    """Fetches movie metadata from TMDB with retry logic."""
    url = f"{TMDB_BASE_URL}/movie/{tmdb_id}?append_to_response=keywords,credits&api_key={TMDB_API_KEY}"
    try:
        response = await client.get(url, timeout=10.0)
        response.raise_for_status()  # Raise an exception for 4xx or 5xx status codes
        return response.json()
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            logger.warning(f"Movie with TMDB ID {tmdb_id} not found (404). Skipping.")
            return None
        elif e.response.status_code == 429:
            logger.warning(f"Rate limited by TMDB API for {tmdb_id}. Retrying...")
            raise  # Re-raise to trigger backoff
        else:
            logger.error(f"HTTP error for TMDB ID {tmdb_id}: {e}")
            raise  # Re-raise for other HTTP errors to trigger backoff
    except httpx.RequestError as e:
        logger.error(f"Network error for TMDB ID {tmdb_id}: {e}. Retrying...")
        raise  # Re-raise for network errors to trigger backoff

async def run_enricher(reset: bool = False, summary: bool = False):
    """
    Main function to run the TMDB metadata enrichment pipeline.

    Args:
        reset (bool): If True, ignores the checkpoint and starts fetching from scratch.
        summary (bool): If True, prints statistics of the generated parquet file.
    """
    DATA_PROCESSED_PATH.mkdir(parents=True, exist_ok=True)

    if summary:
        if TMDB_METADATA_FILE.exists():
            df = pd.read_parquet(TMDB_METADATA_FILE)
            logger.info(f"TMDB Metadata Summary ({TMDB_METADATA_FILE.name}):")
            logger.info(f"  Total records: {len(df)}")
            logger.info(f"  Columns: {df.columns.tolist()}")
            logger.info(f"  Missing keywords: {df['keywords'].apply(lambda x: not x.strip()).sum()}")
            logger.info(f"  Missing director: {df['director'].apply(lambda x: not x.strip()).sum()}")
            logger.info(f"  Missing cast: {df['cast'].apply(lambda x: not x.strip()).sum()}")
        else:
            logger.info("TMDB metadata file does not exist. Run the enricher first.")
        return

    links_df = pd.read_csv(LINKS_FILE)
    if 'tmdbId' not in links_df.columns:
        logger.error(f"'{LINKS_FILE}' must contain a 'tmdbId' column.")
        return

    # Ensure tmdbId is integer for consistency
    links_df['tmdbId'] = links_df['tmdbId'].dropna().astype(int)
    movie_id_map = links_df.set_index('tmdbId')['movieId'].to_dict()
    all_tmdb_ids = links_df['tmdbId'].dropna().astype(int).tolist()

    if reset:
        logger.info("Reset flag active. Ignoring checkpoint and existing metadata.")
        if CHECKPOINT_FILE.exists():
            os.remove(CHECKPOINT_FILE)
            logger.info("Removed existing checkpoint file.")
        if TMDB_METADATA_FILE.exists():
            os.remove(TMDB_METADATA_FILE)
            logger.info("Removed existing TMDB metadata file.")
        processed_tmdb_ids = set()
        tmdb_metadata_list = []
    else:
        processed_tmdb_ids = set(load_checkpoint())
        if TMDB_METADATA_FILE.exists():
            existing_df = pd.read_parquet(TMDB_METADATA_FILE)
            tmdb_metadata_list = existing_df.to_dict('records')
            logger.info(f"Loaded {len(tmdb_metadata_list)} existing records from {TMDB_METADATA_FILE.name}.")
        else:
            tmdb_metadata_list = []

    to_process_tmdb_ids = sorted(list(set(all_tmdb_ids) - processed_tmdb_ids))
    logger.info(f"Starting TMDB enrichment for {len(to_process_tmdb_ids)} new movies out of {len(all_tmdb_ids)} total.")

    last_request_time = 0
    checkpoint_counter = 0

    async with httpx.AsyncClient() as client:
        for i, tmdb_id in enumerate(to_process_tmdb_ids):
            current_time = time.monotonic()
            elapsed = current_time - last_request_time
            if elapsed < RATE_LIMIT_DELAY:
                await_time = RATE_LIMIT_DELAY - elapsed
                time.sleep(await_time)
            last_request_time = time.monotonic()

            logger.info(f"Processing {i+1}/{len(to_process_tmdb_ids)}: TMDB ID {tmdb_id}")
            try:
                raw_data = await fetch_movie_metadata(client, tmdb_id)
                if raw_data:
                    extracted_data = extract_tmdb_data(tmdb_id, raw_data)
                    # Add movieId from links.csv mapping
                    extracted_data['movieId'] = movie_id_map.get(tmdb_id)
                    tmdb_metadata_list.append(extracted_data)
                processed_tmdb_ids.add(tmdb_id)
                checkpoint_counter += 1

                if checkpoint_counter >= 100:
                    save_checkpoint(processed_tmdb_ids)
                    logger.info("Checkpoint saved after 100 movies.")
                    checkpoint_counter = 0

            except Exception as e:
                logger.error(f"Failed to process TMDB ID {tmdb_id} after retries: {e}")
                # Don't add to processed_tmdb_ids, so it can be retried in next run
                save_checkpoint(processed_tmdb_ids) # Save current progress even on error
                break # Stop processing on a persistent error

    if tmdb_metadata_list:
        final_df = pd.DataFrame(tmdb_metadata_list)
        # Ensure movieId is present and convert to int
        final_df = final_df.dropna(subset=['movieId']).astype({'movieId': 'int'})
        # Remove duplicates, preferring the latest processed entry if any TMDB ID was processed multiple times
        final_df.drop_duplicates(subset=['tmdbId'], keep='last', inplace=True)
        final_df.to_parquet(TMDB_METADATA_FILE, index=False)
        logger.info(f"TMDB metadata saved to {TMDB_METADATA_FILE.name} with {len(final_df)} records.")
    else:
        logger.info("No TMDB metadata to save.")

    save_checkpoint(processed_tmdb_ids) # Final checkpoint

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="TMDB metadata enrichment script.")
    parser.add_argument("--reset", action="store_true", help="Ignore checkpoint and existing metadata, start from scratch.")
    parser.add_argument("--summary", action="store_true", help="Show statistics of the generated parquet file.")
    args = parser.parse_args()

    # Use asyncio.run for top-level await
    import asyncio
    asyncio.run(run_enricher(reset=args.reset, summary=args.summary))
