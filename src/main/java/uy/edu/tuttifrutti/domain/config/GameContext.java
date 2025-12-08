package uy.edu.tuttifrutti.domain.config;

public class GameContext {

    private static final GameContext INSTANCE = new GameContext();

    private GameSetup gameSetup;

    private GameContext() {}

    public static GameContext getInstance() {
        return INSTANCE;
    }

    public GameSetup getGameSetup() {
        return gameSetup;
    }

    public void setGameSetup(GameSetup gameSetup) {
        this.gameSetup = gameSetup;
    }
}
