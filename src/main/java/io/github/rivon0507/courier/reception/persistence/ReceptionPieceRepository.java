package io.github.rivon0507.courier.reception.persistence;

import io.github.rivon0507.courier.reception.domain.Reception;
import io.github.rivon0507.courier.reception.domain.ReceptionPiece;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReceptionPieceRepository extends JpaRepository<ReceptionPiece, Long> {
    List<ReceptionPiece> findAllByReceptionAndIdIn(Reception reception, Collection<Long> id);

    Page<ReceptionPiece> findAllByReception(Reception reception, Pageable page);

    void deleteAllByReceptionAndIdIn(Reception reception, Collection<Long> id);
}
