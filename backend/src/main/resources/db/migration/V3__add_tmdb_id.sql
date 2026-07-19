-- V3: Add tmdb_id column to movie table
ALTER TABLE movie ADD COLUMN tmdb_id INTEGER;
CREATE INDEX idx_movie_tmdb_id ON movie(tmdb_id);
