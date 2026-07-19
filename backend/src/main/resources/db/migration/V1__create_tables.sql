-- V1: Create core tables for NextScene
-- ========================================

-- Users table
CREATE TABLE app_user (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    interaction_count INTEGER NOT NULL DEFAULT 0
);

-- User genre preferences (element collection)
CREATE TABLE app_user_genres_preference (
    app_user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    genres_preference VARCHAR(100) NOT NULL
);
CREATE INDEX idx_user_genres_user_id ON app_user_genres_preference(app_user_id);

-- Movies table
CREATE TABLE movie (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id    INTEGER UNIQUE,
    title       VARCHAR(500) NOT NULL,
    genres      VARCHAR(500),
    year        INTEGER,
    poster_url  TEXT,
    synopsis    TEXT,
    cast_list   TEXT,
    rating      DOUBLE PRECISION
);
CREATE INDEX idx_movie_movie_id ON movie(movie_id);
CREATE INDEX idx_movie_genres ON movie(genres);

-- Ratings table
CREATE TABLE rating (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    movie_id    UUID NOT NULL REFERENCES movie(id) ON DELETE CASCADE,
    score       REAL NOT NULL DEFAULT 0,
    avaliacao   VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_rating_user ON rating(user_id);
CREATE INDEX idx_rating_movie ON rating(movie_id);
CREATE UNIQUE INDEX idx_rating_user_movie ON rating(user_id, movie_id);

-- Watchlist table
CREATE TABLE watch_list (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    movie_id    UUID NOT NULL REFERENCES movie(id) ON DELETE CASCADE,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_watchlist_user ON watch_list(user_id);
CREATE UNIQUE INDEX idx_watchlist_user_movie ON watch_list(user_id, movie_id);
