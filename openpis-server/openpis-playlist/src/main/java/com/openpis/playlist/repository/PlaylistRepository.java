package com.openpis.playlist.repository;

import com.openpis.playlist.entity.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    Page<Playlist> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Playlist> findByType(String type, Pageable pageable);

    Page<Playlist> findByNameContainingIgnoreCaseAndType(String name, String type, Pageable pageable);
}
