package uy.edu.tuttifrutti.domain.juez;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.RoundSubmission;

public interface JudgeStrategy {

    JudgeResult juzgar(RoundSubmission submission, GameConfig config);
}
