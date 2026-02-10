package io.github.rivon0507.courier.common.persistence;

import io.github.rivon0507.courier.common.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
