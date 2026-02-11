package io.github.rivon0507.courier.reception.persistence;

import io.github.rivon0507.courier.reception.domain.Reception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceptionRepository extends JpaRepository<Reception, Long> {
}
