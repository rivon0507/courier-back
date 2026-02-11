package io.github.rivon0507.courier.reception.persistence;

import io.github.rivon0507.courier.reception.domain.ReceptionPiece;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceptionPieceRepository extends JpaRepository<ReceptionPiece, Long> {
}
