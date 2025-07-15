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

import java.util.*;
import static jsclub.codefest.sdk.algorithm.PathUtils.distance;

public class PathFinderService {

    static int[] dx = {0, 0, 1, -1};
    static int[] dy = {1,-1,0,0};

    public static Map<String, Node> mp = new HashMap<String, Node>() ;
    public static String findPathToNearestGun(GameMap gameMap, Player player, List<Node> nodesToAvoid, String gunException) {
        Weapon nearestGun = getNearestGun(gameMap, player, gunException);
        if (nearestGun == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestGun, true);
    }

    public static String findPathToNearestWeapon(GameMap gameMap, Player player, List<Node> nodesToAvoid, String meleeException) {
        Weapon nearestWeapon = getNearestWeapon(gameMap, player, meleeException);
        if (nearestWeapon == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestWeapon, true);
    }

    public static String findPathToSupportItem(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        SupportItem nearestHealingItem = getNearestSupportItem(gameMap, player);

        if (nearestHealingItem == null ) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestHealingItem, true);
    }

    public static String findPathToNearestOtherPlayer(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        Player nearestEnemy = getNearestOtherPlayer(gameMap, player);
        if (nearestEnemy == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestEnemy, true);
    }

    public static String findPathToNearestChest(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        Obstacle nearestChest = getNearestChest(gameMap, player);
        if (nearestChest == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestChest, true);
    }

    public static String findPathToAroundItem(GameMap gameMap, Player player, List<Node> nodesToAvoid) {
        Element nearestItem = getAroundItem(gameMap, player);
        if (nearestItem == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestItem, true);
    }

    public static String findPathToNearestArmor(GameMap gameMap, Player player, List<Node> avoid, ArrayList<String> exceptions) {
        Armor armor = getNearestArmor(gameMap, player, exceptions);
        if (armor == null) return null;
        return PathUtils.getShortestPath(gameMap, avoid, player, armor, true);
    }

    public static boolean checkIsObstacle(Node x, GameMap gameMap) {
        Element e = gameMap.getElementByIndex(x.x, x.y);
        ElementType type = e.getType();
        return type == ElementType.CHEST || type == ElementType.TRAP || type == ElementType.INDESTRUCTIBLE;
    }

    public static Node loang(int[][] a, GameMap gameMap, int x, int y, int safeZone, int mapSize) {
        boolean[][] visited = new boolean[mapSize+1][mapSize+1];
        Queue<int[]> queue = new LinkedList<>();

        queue.offer(new int[]{x, y});
        visited[x][y] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x1 = current[0], y1 = current[1];
            Node node1 = new Node(x1,y1);
            if (PathUtils.checkInsideSafeArea(node1, safeZone, mapSize) && !checkIsObstacle(node1, gameMap)) {
                return node1;
            }

            for (int k = 0; k < 4; k++) {
                int nx = x1 + dx[k];
                int ny = y1 + dy[k];
                Node newNode = new Node(nx, ny);
                if (nx >= 0 && ny >= 0 && nx < mapSize && ny < mapSize &&
                        !visited[nx][ny] && !checkIsObstacle(newNode, gameMap)) {
                    visited[nx][ny] = true;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }

        return null; // Không tìm thấy safezone
    }



    public static String getPathToNearestNodeInsideSafeZone(GameMap gameMap, Player player, List<Node> avoid) {
        int[][] a = new int[105][105];
        Node currentPlayerPosition = player.getPosition();
        a[currentPlayerPosition.x][currentPlayerPosition.y] = 1;
        Node safeZoneNode = loang(a, gameMap, currentPlayerPosition.x, currentPlayerPosition.y, gameMap.getSafeZone(), gameMap.getMapSize());
        System.out.println("tọa độ" + " " + safeZoneNode.getX() + " " + safeZoneNode.getY());
        String shortestPathToSafeZone = "";
        if (safeZoneNode == null) {
            System.out.println("PATH BY LOANG NOT FOUND");
            shortestPathToSafeZone = PathUtils.getShortestPath(gameMap, avoid, player, getNearestOtherPlayer(gameMap, player), true);
        } else {
            shortestPathToSafeZone = PathUtils.getShortestPath(gameMap, avoid, player, safeZoneNode, true);
        }
        return shortestPathToSafeZone;
    }

    public static Weapon getNearestWeapon(GameMap gameMap, Player player, String meleeExcetion) {
        List<Weapon> weapons = gameMap.getListWeapons();
        Weapon nearestWeapon = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon weapon : weapons) {
            if(weapon.getType().name() != ElementType.GUN.name()) {
                double distance = distance(player, weapon);
                if (distance < minDistance && PathUtils.checkInsideSafeArea(weapon.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize()) && !weapon.getId().equals(meleeExcetion)) {
                    minDistance = distance;
                    nearestWeapon = weapon;
                }
            }
        }
        return nearestWeapon;
    }

    public static SupportItem getNearestSupportItem  (GameMap gameMap, Player player) {
        List<SupportItem> SupportItems = gameMap.getListSupportItems();
        SupportItem nearestSupportItem = null;
        double minDistance = Double.MAX_VALUE;

        for (SupportItem SupportItem : SupportItems) {
            double distance = distance(player, SupportItem);
            if (distance < minDistance && PathUtils.checkInsideSafeArea(SupportItem.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize()) ) {
                minDistance = distance;
                nearestSupportItem = SupportItem;
            }
        }
        return nearestSupportItem;
    }

    public static Weapon getNearestGun(GameMap gameMap, Player player, String gunException) {
        List<Weapon> guns = gameMap.getAllGun();
        Weapon nearestGun = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon gun : guns) {
            double distance = distance(player, gun);
            if (distance < minDistance && PathUtils.checkInsideSafeArea(gun.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize()) && !gun.getId().equals(gunException)) {
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
            double distance = distance(player, chest);
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

    public static Enemy getNearestEnemy(GameMap gameMap, Player player) {
        Enemy nearestEnemy = null;
        double minDistance = Double.MAX_VALUE;

        for (Enemy enemy : gameMap.getListEnemies()) {
            double distance = distance(player, enemy);
            if (distance < minDistance && PathUtils.checkInsideSafeArea(enemy.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize())) {
                minDistance = distance;
                nearestEnemy = enemy;
            }
        }
        return nearestEnemy;
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

    public static Armor getNearestArmor(GameMap gameMap, Player player, ArrayList<String> exceptions) {
        Armor armor = null;
        double minDist = Double.MAX_VALUE;
        for (Armor a : gameMap.getListArmors()) {
            double d = distance(player, a);
            if (d < minDist && PathUtils.checkInsideSafeArea(a.getPosition(), gameMap.getSafeZone(), gameMap.getMapSize()) && !exceptions.contains(a.getId())) {
                minDist = d;
                armor = a;
            }
        }
        return armor;
    }

    public static int getDistanceToNearestWeapon(GameMap gameMap, Player player, String meleeException, List<Node> avoid) {
        Weapon weapon = getNearestWeapon(gameMap, player, meleeException);
        if(weapon != null)
            return PathUtils.getShortestPath(gameMap, avoid, player, weapon, true).length();
        else
            return Integer.MAX_VALUE;
    }

    public static int getDistanceToNearestArmor(GameMap gameMap, Player player, ArrayList<String> exception, List<Node> avoid) {
        Armor armor = getNearestArmor(gameMap, player, exception);
        if(armor != null)
            return PathUtils.getShortestPath(gameMap, avoid, player, armor, true).length();
        else
            return Integer.MAX_VALUE;
    }


    public static int getDistanceToNearestOtherPlayer(GameMap gameMap, Player player) {
        Player enemy = getNearestOtherPlayer(gameMap, player);
        if(enemy != null && enemy.getHealth() > 0)
            return distance(player, enemy);
        else return Integer.MAX_VALUE;
    }

    public static int getDistanceToNearestGun(GameMap gameMap, Player player, String gunException, List<Node> avoid) {
        Weapon gun = getNearestGun(gameMap, player, gunException);
        if(gun != null) return  PathUtils.getShortestPath(gameMap, avoid, player, gun, true).length();
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

    public static String optimizeShortestPath(String path) {
        if (checkSingleDirection(path)) return path;
        boolean hasU = false, hasD = false, hasL = false, hasR = false;
        for(int i = 0; i < path.length(); i++) {
            if(path.charAt(i) == 'u') hasU = true;
            if(path.charAt(i) == 'd') hasD = true;
            if(path.charAt(i) == 'l') hasL = true;
            if(path.charAt(i) == 'r') hasR = true;
        }
        if(hasU && hasD) return path;
        if(hasL && hasR) return path;
        int ngang = 0, doc = 0;
        char ngangChar = 'l', docChar = 'u';
        for(int i = 0; i < path.length(); i++) {
            if(path.charAt(i) == 'u' || path.charAt(i) == 'd') {
                doc++;
                docChar = path.charAt(i);
            }
            if(path.charAt(i) == 'l' || path.charAt(i) == 'r') {
                ngang++;
                ngangChar = path.charAt(i);
            }
        }
        String result = "";
        StringBuilder sb = new StringBuilder(result);
        boolean minSideIsNgang = ngang <= doc;
        for(int i = 0; i < Math.min(ngang,doc); i++) {
            if(minSideIsNgang) sb.append(ngangChar);
            else sb.append(docChar);
        }
        for(int i = 0; i < Math.max(ngang,doc); i++) {
            if(minSideIsNgang) sb.append(docChar);
            else sb.append(ngangChar);
        }
        String res = sb.toString();
        System.out.println("Path before optimize: " + path + "\n" + "Path after optimized: " + res);
        return res;
    }


    public static boolean checkSingleDirection(String path) {
        for(int i = 0; i < path.length() - 1; i++) {
            if(path.charAt(i) != path.charAt(i+1)) {
                return false;
            }
        }
        return true;
    }
}

