package com.openpis.program.repository;

import com.openpis.program.entity.Program;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    Page<Program> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
