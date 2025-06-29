package services;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;

import java.io.IOException;
import java.util.List;

public class GameActionHandler {
    private final Hero hero;

    public GameActionHandler(Hero hero) {
        this.hero = hero;
    }

    public void perform(GameMap gameMap) throws IOException {
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == 0) return;

        List<Node> avoid = NodeAvoidanceUtil.computeNodesToAvoid(gameMap);
        Inventory inventory = hero.getInventory();
        if (inventory.getGun() == null || inventory.getMelee() == null) {
            searchForChestAndLoot(gameMap, player, avoid);
        } else searchForEnemy(gameMap, player, avoid);
    }

    private void searchForGun(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        String path = PathFinderService.findPathToNearestGun(gameMap, player, avoid);
        if (path == null) return;
        if (path.isEmpty()) hero.pickupItem();
        else hero.move(path);
    }

    private void searchForHealingItem(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        String path = PathFinderService.findPathToHealingItem(gameMap, player, avoid);
        if (path == null) return;
        if (path.isEmpty()) hero.pickupItem();
        else hero.move(path);
    }


    private void searchForEnemy(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        String path = PathFinderService.findPathToNearestOtherPlayer(gameMap, player, avoid);
        if (path == null) return;
        int distanceToEnemy = PathFinderService.getDistanceToNearestOtherPlayer(gameMap, player);
        if(hero.getInventory().getGun().getRange() >=  distanceToEnemy){
            hero.shoot(path.charAt(path.length()-1) + "");
        } else hero.move(path);
    }

    private void searchForChestAndLoot(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        String path = PathFinderService.findPathToNearestChest(gameMap, player, avoid);
        if (path == null) return;
        int distanceToChest = PathFinderService.getDistanceToNearestChest(gameMap, player);
        if (distanceToChest <= hero.getInventory().getMelee().getRange()) {
            hero.attack(path.charAt(path.length() - 1) + "");
            if(distanceToChest > hero.getInventory().getMelee().getRange()){
                while(PathFinderService.getAroundItem(gameMap,player) != null){
                    String pathToItem = PathFinderService.findPathToAroundItem(gameMap, player, avoid);
                    if (pathToItem == null) break;
                    if (pathToItem.isEmpty()) hero.pickupItem();
                    else hero.move(path);

                }
            }
        }
        else hero.move(path);
    }

}
