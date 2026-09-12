package com.openpis.playlist.repository;

import com.openpis.playlist.entity.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {

    List<PlaylistItem> findByPlaylistIdOrderBySortOrderAsc(Long playlistId);

    void deleteByPlaylistId(Long playlistId);
}
