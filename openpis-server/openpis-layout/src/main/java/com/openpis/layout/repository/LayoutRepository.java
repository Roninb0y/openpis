package com.openpis.layout.repository;

import com.openpis.layout.entity.Layout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LayoutRepository extends JpaRepository<Layout, Long> {

    Page<Layout> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
