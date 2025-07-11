



import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import services.GameActionHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "118391";
    private static final String PLAYER_NAME = "<ProPTIT/> Quantum5";
    private static final String SECRET_KEY = "sk-5cpqGGy9QuyhUHC9F-abfg:ot6VfGZ6ukwEXEJFAiaik2R3nBBppoTRFEtgNLIdxDY83aamgJ2UB9q9LyM6ocRcIg68vtqoXNAbPU023f9-mA";



    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener onMapUpdate = new MapUpdateListener(hero);

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;
    private final GameActionHandler actionHandler;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
        this.actionHandler = new GameActionHandler(hero);
    }

    @Override
    public void call(Object... args) {
        if (args == null || args.length == 0) return;
        try {
            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            actionHandler.perform(gameMap);
        } catch (Exception e) {
            System.err.println("Error in MapUpdateListener: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


