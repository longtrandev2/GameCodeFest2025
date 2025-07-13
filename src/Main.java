



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
    private static final String GAME_ID = "113364";
    private static final String PLAYER_NAME = "<ProPTIT/> Quantum5";
    private static final String SECRET_KEY = "sk-DUZvyG1bQOGZtVSUtmjPpQ:hN8Gq_jym5F9VsZvIGI6RZ1Zj5-y1ploU5VFP0ZwWnAZxq52QEgms0IVCN45sRQqRWXYuAIHP0phs7e4gp7i0w";


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


