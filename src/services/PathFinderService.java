package services;

import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
//import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.socket.data.receive_data.ItemData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jsclub.codefest.sdk.algorithm.PathUtils.distance;

public class PathFinderService {
    public static Map<String, Node> mp = new HashMap<String, Node>() ;
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

    public static String findPathToSupportItem(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        SupportItem nearestHealingItem = getNearestSupportItem(gameMap, player);
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

    public static String findPathToNearestArmor(GameMap gameMap, Player player, List<Node> avoid) {
        Armor armor = getNearestArmor(gameMap, player);
        if (armor == null) return null;
        return PathUtils.getShortestPath(gameMap, avoid, player, armor, false);
    }


    public static Weapon getNearestWeapon(GameMap gameMap, Player player) {
        List<Weapon> weapons = gameMap.getListWeapons();
        Weapon nearestWeapon = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon weapon : weapons) {
            if(weapon.getType().name() != ElementType.GUN.name()) {
                double distance = PathUtils.distance(player, weapon);
                if (distance < minDistance && PathUtils.checkInsideSafeArea(weapon.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize())) {
                    minDistance = distance;
                    nearestWeapon = weapon;
                }
            }
        }
        return nearestWeapon;
    }

    private static SupportItem getNearestSupportItem  (GameMap gameMap, Player player) {
        List<SupportItem> SupportItems = gameMap.getListSupportItems();
        SupportItem nearestSupportItem = null;
        double minDistance = Double.MAX_VALUE;

        for (SupportItem SupportItem : SupportItems) {
            double distance = PathUtils.distance(player, SupportItem);
            if (distance < minDistance) {
                minDistance = distance;
                nearestSupportItem = SupportItem;
            }
        }
        return nearestSupportItem;
    }

    public static Weapon getNearestGun(GameMap gameMap, Player player) {
        List<Weapon> guns = gameMap.getAllGun();
        Weapon nearestGun = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon gun : guns) {
            double distance = PathUtils.distance(player, gun);
            if (distance < minDistance && PathUtils.checkInsideSafeArea(gun.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize())) {
                minDistance = distance;
                nearestGun = gun;
            }
        }
        return nearestGun;
    }

    public static Obstacle getNearestChest(GameMap gameMap, Player player) {
        List<Obstacle> chests = gameMap.getObstaclesByTag("DESTRUCTIBLE");
        Obstacle nearestChest = null;
        double minDistance = Double.MAX_VALUE;
        System.out.println(chests);
        for (Obstacle chest : chests) {
            double distance = PathUtils.distance(player, chest);
            if (distance < minDistance && PathUtils.checkInsideSafeArea(chest.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize())) {
                minDistance = distance;
                nearestChest = chest;
            }
        }
        return nearestChest;
    }

    public static Player getNearestOtherPlayer(GameMap gameMap, Player player) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        if(gameMap.getOtherPlayerInfo().isEmpty()) return null;
        for (Player e : gameMap.getOtherPlayerInfo()) {
            double d = distance(player, e);
            if (d < minDist && PathUtils.checkInsideSafeArea(e.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize())) {
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

    public static Armor getNearestArmor(GameMap gameMap, Player player) {
        Armor armor = null;
        double minDist = Double.MAX_VALUE;
        for (Armor a : gameMap.getListArmors()) {
            double d = distance(player, a);
            if (d < minDist && PathUtils.checkInsideSafeArea(a.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize())) {
                minDist = d;
                armor = a;
            }
        }
        return armor;
    }

    public static int getDistanceToNearestWeapon(GameMap gameMap, Player player) {
        Weapon weapon = getNearestWeapon(gameMap, player);
        if(weapon != null)
            return distance(player, getNearestWeapon(gameMap, player));
        else
            return Integer.MAX_VALUE;
    }

    public static int getDistanceToNearestArmor(GameMap gameMap, Player player) {
        Armor armor = getNearestArmor(gameMap, player);
        if(armor != null)
            return distance(player, getNearestArmor(gameMap, player));
        else
            return Integer.MAX_VALUE;
    }

    public static int getDistanceToNearestOtherPlayer(GameMap gameMap, Player player) {
        Player enemy = getNearestOtherPlayer(gameMap, player);
        if(enemy != null)
            return distance(player, getNearestOtherPlayer(gameMap, player));
        else return Integer.MAX_VALUE;
    }

    public static int getDistanceToNearestGun(GameMap gameMap, Player player) {
        Weapon gun = getNearestGun(gameMap, player);
        if(gun != null) return distance(player, getNearestGun(gameMap, player));
        else return Integer.MAX_VALUE;
    }
    public static int getDistanceToNearestChest(GameMap gameMap, Player player) {
        Obstacle chest = getNearestChest(gameMap, player);
        if(chest != null)
            return distance(player, getNearestChest(gameMap, player));
        else return Integer.MAX_VALUE;
    }

    public static int getDistanceToSupportItem(GameMap gameMap, Player player) {
        SupportItem supportItem = getNearestSupportItem(gameMap, player);
        if(supportItem != null)
            return distance(player, getNearestSupportItem(gameMap, player));
        else return Integer.MAX_VALUE;
    }
}

