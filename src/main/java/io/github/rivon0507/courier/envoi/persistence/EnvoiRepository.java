package io.github.rivon0507.courier.envoi.persistence;

import io.github.rivon0507.courier.envoi.domain.Envoi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnvoiRepository extends JpaRepository<Envoi, Long> {
    Page<Envoi> findAllByWorkspace_Id(Long workspaceId, Pageable page);

    Optional<Envoi> findByIdAndWorkspace_Id(Long id, Long workspaceId);

    void deleteByIdAndWorkspace_Id(Long id, Long workspaceId);
}
