package io.nextscene.backend.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(1) // Run before DataSeeder to ensure movies exist
@RequiredArgsConstructor
public class MovieImporter implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movie", Integer.class);
        Integer unsplashCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM movie WHERE poster_url LIKE '%unsplash%'", Integer.class);
        boolean hasUnsplash = unsplashCount != null && unsplashCount > 0;

        if (count != null && count > 15 && !hasUnsplash) {
            log.info("ℹ️ Banco de dados já possui {} filmes. Pulando importação de catálogo.", count);
            return;
        }

        log.info("🎬 Iniciando importação do catálogo MovieLens (movies.csv + links.csv)...");

        // 1. Load links mapping: movieId -> tmdbId
        Map<Integer, Integer> linksMap = new HashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/links.csv").getInputStream()))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) {
                    try {
                        int movieId = Integer.parseInt(parts[0]);
                        if (!parts[2].trim().isEmpty()) {
                            int tmdbId = Integer.parseInt(parts[2].trim());
                            linksMap.put(movieId, tmdbId);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        log.info("✅ Carregados {} mapeamentos de links.", linksMap.size());

        // 2. Load and parse movies
        List<MovieImportRecord> records = new ArrayList<>();
        Pattern yearPattern = Pattern.compile("\\((\\d{4})\\)\\s*$");

        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/movies.csv").getInputStream()))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }

                // Correct parsing of MovieLens movies.csv (handles commas in quoted titles)
                List<String> parts = parseCsvLine(line);
                if (parts.size() >= 3) {
                    try {
                        int movieId = Integer.parseInt(parts.get(0));
                        String rawTitle = parts.get(1);
                        String genres = parts.get(2).replace("|", ", ");

                        // Extract year from title
                        Integer year = null;
                        String cleanTitle = rawTitle;
                        Matcher matcher = yearPattern.matcher(rawTitle);
                        if (matcher.find()) {
                            year = Integer.parseInt(matcher.group(1));
                            cleanTitle = rawTitle.replaceAll("\\((\\d{4})\\)\\s*$", "").trim();
                        }
                        
                        // Clean up quotes if title was quoted
                        if (cleanTitle.startsWith("\"") && cleanTitle.endsWith("\"")) {
                            cleanTitle = cleanTitle.substring(1, cleanTitle.length() - 1).trim();
                        }

                        Integer tmdbId = linksMap.get(movieId);
                        // Default placeholder poster url showing the movie's title in a gold-on-dark themed placeholder card
                        String posterUrl = "https://placehold.co/600x900/1a1a1a/e2b63c?text=" + 
                                java.net.URLEncoder.encode(cleanTitle, java.nio.charset.StandardCharsets.UTF_8);

                        records.add(new MovieImportRecord(movieId, tmdbId, cleanTitle, genres, year, posterUrl));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        log.info("⏳ Salvando {} filmes no banco via Batch Insert...", records.size());

        // Clear existing movies to avoid duplicate keys with seeded mock movies
        jdbcTemplate.update("DELETE FROM watch_list");
        jdbcTemplate.update("DELETE FROM rating");
        jdbcTemplate.update("DELETE FROM movie");

        String sql = "INSERT INTO movie (id, movie_id, tmdb_id, title, genres, year, poster_url, rating) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MovieImportRecord r = records.get(i);
                ps.setObject(1, UUID.randomUUID());
                ps.setInt(2, r.movieId());
                if (r.tmdbId() != null) ps.setInt(3, r.tmdbId());
                else ps.setNull(3, java.sql.Types.INTEGER);
                ps.setString(4, r.title());
                ps.setString(5, r.genres());
                if (r.year() != null) ps.setInt(6, r.year());
                else ps.setNull(6, java.sql.Types.INTEGER);
                ps.setString(7, r.posterUrl());
                ps.setDouble(8, 0.0); // Will enrich rating/poster dynamically
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });

        log.info("✅ Catálogo importado com sucesso! Total: {} filmes.", records.size());
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder curVal = new StringBuilder();
        boolean inQuotes = false;
        char[] chars = line.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < chars.length && chars[i+1] == '\"') {
                        curVal.append('\"'); // escaped quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    curVal.append(ch);
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    result.add(curVal.toString().trim());
                    curVal.setLength(0);
                } else {
                    curVal.append(ch);
                }
            }
        }
        result.add(curVal.toString().trim());
        return result;
    }

    private record MovieImportRecord(
            int movieId,
            Integer tmdbId,
            String title,
            String genres,
            Integer year,
            String posterUrl
    ) {}
}
