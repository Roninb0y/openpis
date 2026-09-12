package com.openpis.player.repository;

import com.openpis.player.entity.PlayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayLogRepository extends JpaRepository<PlayLog, Long> {

    @Query(value = "SELECT DATE(started_at) AS d, COUNT(*) FROM play_log " +
            "WHERE started_at >= :since GROUP BY DATE(started_at) ORDER BY d", nativeQuery = true)
    List<Object[]> countDaily(@Param("since") LocalDateTime since);

    @Query(value = "SELECT l.program_id, COALESCE(p.name, '已删除'), COUNT(*) FROM play_log l " +
            "LEFT JOIN program p ON p.id = l.program_id " +
            "GROUP BY l.program_id, p.name ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countByProgram();

    @Query(value = "SELECT l.material_id, COALESCE(m.title, '已删除'), COUNT(*) FROM play_log l " +
            "LEFT JOIN material m ON m.id = l.material_id " +
            "GROUP BY l.material_id, m.title ORDER BY COUNT(*) DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> countByMaterial(@Param("limit") int limit);
}
