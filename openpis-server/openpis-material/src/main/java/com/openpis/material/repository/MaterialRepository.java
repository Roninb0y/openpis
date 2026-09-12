package com.openpis.material.repository;

import com.openpis.material.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    Page<Material> findByType(String type, Pageable pageable);

    Page<Material> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Material> findByTypeAndTitleContainingIgnoreCase(String type, String keyword, Pageable pageable);
}
