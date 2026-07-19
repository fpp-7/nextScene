package io.nextscene.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "app_user_genres_preference",
            joinColumns = @JoinColumn(name = "app_user_id")
    )
    @Column(name = "genres_preference")
    private List<String> genresPreference = new ArrayList<>();

    @Column(name = "interaction_count", nullable = false)
    private Integer interactionCount = 0;
}
