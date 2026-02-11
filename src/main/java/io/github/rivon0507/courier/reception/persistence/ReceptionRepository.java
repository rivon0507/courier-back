package io.github.rivon0507.courier.reception.persistence;

import io.github.rivon0507.courier.reception.domain.Reception;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceptionRepository extends JpaRepository<Reception, Long> {
    Optional<Reception> findByIdAndWorkspace_Id(Long id, Long workspaceId);

    Page<Reception> findAllByWorkspace_Id(Long workspaceId, Pageable page);

    void deleteByIdAndWorkspace_Id(Long id, Long workspaceId);
}
