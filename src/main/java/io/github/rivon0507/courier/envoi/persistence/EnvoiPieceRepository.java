package io.github.rivon0507.courier.envoi.persistence;

import io.github.rivon0507.courier.envoi.domain.EnvoiPiece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvoiPieceRepository extends JpaRepository<EnvoiPiece, Long> {
}
