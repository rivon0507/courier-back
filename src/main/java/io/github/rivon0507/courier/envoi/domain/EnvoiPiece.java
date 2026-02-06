package io.github.rivon0507.courier.envoi.domain;


import io.github.rivon0507.courier.common.domain.Piece;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "envoi_pieces")
public class EnvoiPiece extends Piece {
    @ManyToOne
    @JoinColumn(name = "envoi_id", nullable = false)
    private Envoi envoi;
}
