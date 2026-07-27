package io.nextscene.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "movie_id", unique = true)
    private Integer movieId;

    @Column(name = "tmdb_id")
    private Integer tmdbId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String genres;

    private Integer year;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "cast_list", columnDefinition = "TEXT")
    private String castList;

    private Double rating;

    /** Quando o filme foi enriquecido via TMDB. Null = pendente para o job. */
    @Column(name = "enriched_at")
    private Instant enrichedAt;
}
