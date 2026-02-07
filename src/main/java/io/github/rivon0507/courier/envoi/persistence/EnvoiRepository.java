package io.github.rivon0507.courier.envoi.persistence;

import io.github.rivon0507.courier.envoi.domain.Envoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface EnvoiRepository extends JpaRepository<Envoi, Long> {
}
