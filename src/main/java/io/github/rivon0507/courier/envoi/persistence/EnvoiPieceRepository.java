package io.github.rivon0507.courier.envoi.persistence;

import io.github.rivon0507.courier.envoi.domain.Envoi;
import io.github.rivon0507.courier.envoi.domain.EnvoiPiece;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EnvoiPieceRepository extends JpaRepository<EnvoiPiece, Long> {
    List<EnvoiPiece> findAllByEnvoiAndIdIn(Envoi envoi, Collection<Long> ids);

    void deleteAllByEnvoiAndIdIn(Envoi envoi, Collection<Long> ids);

    Page<EnvoiPiece> findAllByEnvoi(Envoi envoi, Pageable pageable);
}
