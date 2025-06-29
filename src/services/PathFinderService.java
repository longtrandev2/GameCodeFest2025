package services;

import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.socket.data.receive_data.ItemData;

import java.util.ArrayList;
import java.util.List;

import static jsclub.codefest.sdk.algorithm.PathUtils.distance;

public class PathFinderService {
    public static String findPathToNearestGun(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        Weapon nearestGun = getNearestGun(gameMap, player);
        if (nearestGun == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestGun, false);
    }

    public static String findPathToNearestWeapon(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        Weapon nearestWeapon = getNearestWeapon(gameMap, player);
        if (nearestWeapon == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestWeapon, false);
    }
    public static String findPathToHealingItem(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        HealingItem nearestHealingItem = getNearestHealingItem(gameMap, player);
        if (nearestHealingItem == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestHealingItem, false);
    }

    public static String findPathToNearestOtherPlayer(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        Player nearestEnemy = getNearestOtherPlayer(gameMap, player);
        if (nearestEnemy == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestEnemy, false);
    }

    public static String findPathToNearestChest(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        Obstacle nearestChest = getNearestChest(gameMap, player);
        if (nearestChest == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestChest, false);
    }

    public static String findPathToAroundItem(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        Element nearestItem = getAroundItem(gameMap, player);
        if (nearestItem == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestItem, false);
    }


    private static Weapon getNearestWeapon(GameMap gameMap, Player player) {
        List<Weapon> weapons = gameMap.getListWeapons();
        Weapon nearestWeapon = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon weapon : weapons) {
            double distance = PathUtils.distance(player, weapon);
            if (distance < minDistance) {
                minDistance = distance;
                nearestWeapon = weapon;
            }
        }
        return nearestWeapon;
    }

    private static HealingItem getNearestHealingItem  (GameMap gameMap, Player player) {
        List<HealingItem> healingItems = gameMap.getListHealingItems();
        HealingItem nearestHealingItem = null;
        double minDistance = Double.MAX_VALUE;

        for (HealingItem healingItem : healingItems) {
            double distance = PathUtils.distance(player, healingItem);
            if (distance < minDistance) {
                minDistance = distance;
                nearestHealingItem = healingItem;
            }
        }
        return nearestHealingItem;
    }

    private static Weapon getNearestGun(GameMap gameMap, Player player) {
        List<Weapon> guns = gameMap.getAllGun();
        Weapon nearestGun = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon gun : guns) {
            double distance = PathUtils.distance(player, gun);
            if (distance < minDistance) {
                minDistance = distance;
                nearestGun = gun;
            }
        }
        return nearestGun;
    }

    private  static Obstacle getNearestChest(GameMap gameMap, Player player) {
        List<Obstacle> chests = gameMap.getListChests();
        Obstacle nearestChest = null;
        double minDistance = Double.MAX_VALUE;
        System.out.println(chests);
        for (Obstacle chest : chests) {
            double distance = PathUtils.distance(player, chest);
            if (distance < minDistance) {
                minDistance = distance;
                nearestChest = chest;
            }
        }
        return nearestChest;
    }

    private static Player getNearestOtherPlayer(GameMap gameMap, Player player) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Player e : gameMap.getOtherPlayerInfo()) {
            double d = distance(player, e);
            if (d < minDist) {
                minDist = d;
                nearest = e;
            }
        }
        return nearest;
    }

    public static Element getAroundItem(GameMap gameMap, Player player) {
        Element aroundItem = null;
        int x = player.getX();
        int y = player.getY();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx;
                int ny = y + dy;


                Element element = gameMap.getElementByIndex(nx, ny);

                if (element != null) {
                    return element;
                }
            }
        }
        return null;
    }

    public static int getDistanceToNearestOtherPlayer(GameMap gameMap, Player player) {
        return distance(player, getNearestOtherPlayer(gameMap, player));
    }

    public static int getDistanceToNearestGun(GameMap gameMap, Player player) {
        return distance(player, getNearestGun(gameMap, player));
    }
    public static int getDistanceToNearestChest(GameMap gameMap, Player player) {

        return distance(player, getNearestChest(gameMap, player));
    }
}

