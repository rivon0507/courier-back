package io.github.rivon0507.courier.reception.domain;

import io.github.rivon0507.courier.common.domain.Piece;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "reception_pieces")
public class ReceptionPiece extends Piece {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Reception reception;
}