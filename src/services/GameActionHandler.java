package services;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.util.SocketUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static jsclub.codefest.sdk.algorithm.PathUtils.distance;

public class GameActionHandler {
    private final Hero hero;

    // Tracking other player
    Player enemyTracked;
    int oldNodeX;
    int oldNodeY;
    private String nextAction = "";
    int counterActionIdle = 0;
    int correctedAction = 0;

    private String enemy_pathToGun = "";
    private String enemy_pathToWeapon = "";
    private String enemy_pathToSupportItem = "";
    private String enemy_pathToChest = "";
    private String enemy_pathToArmor = "";
    private String enemy_pathToPlayer = "";

    private float countEnemyAction_toGun = 0;
    private float countEnemyAction_toWeapon = 0;
    private float countEnemyAction_toSupportItem = 0;
    private float countEnemyAction_toChest = 0;
    private float countEnemyAction_toArmor = 0;
    private float countEnemyAction_toPlayer = 0;
    // end

    // Counter cooldown
    private static double cooldownTimer_gun = 0;
    private static double cooldownTimer_melee = 0;
    private static double counter_cooldown_gun = 0;
    private static double counter_cooldown_melee = 0;
    private boolean canShoot;
    private boolean canAttack;
    private boolean isPickupGun;
    private boolean isPickupMelee;

    private static double coolDownSpecialReamin = 0;

    private static Boolean usedSpecialItems = false;
    //end

    // config time
    private static final int timeAvoid = 480; // 1080

    public enum TaskType {
        GUN,
        CHEST,
        ENEMY,
        ARMOR,
        HELMET,
        SUPPORT_ITEM,
        RUN,
        WEAPON,
        MOVE
    }

    public GameActionHandler(Hero hero) {
        this.hero = hero;
    }

    public void perform(GameMap gameMap) throws IOException {
        ///
        if(isPickupGun && hero.getInventory().getGun() != null){
            isPickupGun = false;
            cooldownTimer_gun = hero.getInventory().getGun().getCooldown();
            counter_cooldown_gun = cooldownTimer_gun;
            canShoot = true;
        }
        if(isPickupMelee && hero.getInventory().getMelee() != null) {
            isPickupMelee = false;
            cooldownTimer_melee = hero.getInventory().getMelee().getCooldown();
            counter_cooldown_melee = cooldownTimer_melee;
            canAttack = true;
        }
        ///


        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == 0) return;

        List<Node> avoid = NodeAvoidanceUtil.computeNodesToAvoid(gameMap);
        Inventory inventory = hero.getInventory();
        // Tìm và sử dụng item phù hợp
//        updateOtherPlayerPosition(gameMap, player, PathFinderService.mp);
        trackingNearestPlayer(gameMap, player, avoid);
        attackingStatus(gameMap, player, avoid);

        if(selectSupportItems(gameMap, hero, player, avoid))
            return;

        // Check trạng thái trong bo
        if (!PathUtils.checkInsideSafeArea(player, gameMap.getSafeZone(), gameMap.getMapSize())) {
            int radiusSafeZone = gameMap.getSafeZone();
            String path = "";

            // Dùng hàm loang, nếu không được kết quả thì fallback dùng như cũ.
            path = PathFinderService.getPathToNearestNodeInsideSafeZone(gameMap, player, avoid);

            // fallback
            if (path != null ||  path.isEmpty()) {
                if(player.getX() <= gameMap.getMapSize()/2 - radiusSafeZone) {
                    path = "r";
                } else if(player.getX() >= gameMap.getMapSize()/2 + radiusSafeZone) {
                    path = "l";
                } else if(player.getY() <= gameMap.getMapSize()/2 - radiusSafeZone) {
                    path = "u";
                } else if(player.getY() >= gameMap.getMapSize()/2 + radiusSafeZone) {
                    path = "d";
                }
                System.out.println(path);
                System.out.println(player.getX() + " " + player.getY() + " " + gameMap.getMapSize()/2);
            }
            hero.move(path);
            return;
        }


        // Xử lý action
        if (inventory.getGun() == null && PathFinderService.findPathToNearestGun(gameMap, player,avoid, "") != null) {
            List<Node> avoidChest = NodeAvoidanceUtil.computeNodesToAvoid(gameMap);
            avoidChest.addAll(gameMap.getObstaclesByTag("DESTRUCTIBLE"));
            searchForGun(gameMap, player, avoidChest);
        } else {
            List<Player> list = gameMap.getOtherPlayerInfo();
            boolean hasEnemy = false;
            for (Player p : list ) {
                if(p.getHealth() != 0) {
//                    searchForEnemy(gameMap, player, avoid);
//                    searchForEnemy_v2(gameMap, player, avoid);
                    hasEnemy = true;
                    break;
                }
            }
            PriorityTask task = selectTask(gameMap, player, avoid, hero, hasEnemy);
            TaskType action = task.name;

            System.out.println("ACTION HERE: " + action.name());
            if (action == TaskType.CHEST) {
                searchForChestAndLoot(gameMap, player, avoid, task.element);
            }
            else if (action == TaskType.ENEMY) {
                if(player.getHealth() > 20) searchForEnemy_v2(gameMap, player, avoid);
                else avoidOtherPlayer_v2(gameMap, player, avoid);
            }
            else if (action == TaskType.ARMOR) {
//                ArrayList<String> exceptions = new ArrayList<>();
                searchForArmorOrHelmet(gameMap, player, avoid, task.element);
            }
            else if (action == TaskType.WEAPON) {
                searchForWeapon(gameMap, player, avoid, task.element);
            }
            else if (action == TaskType.SUPPORT_ITEM) {
                searchForSupportItem(gameMap, player, avoid, task.element);
            }
            else if (action == TaskType.GUN) {
                searchForGun(gameMap, player, avoid, task.element);
            }
            else if (action == TaskType.RUN){
                // avoidOtherPlayer_ShorestPath(gameMap, player, avoid);
                avoidOtherPlayer_v2(gameMap, player, avoid);
            }
        }
        if(usedSpecialItems && coolDownSpecialReamin >= 1 ) {
            coolDownSpecialReamin -= 1;
        } else usedSpecialItems = false;
    }

    private PriorityTask selectTask(GameMap gameMap, Player player, List<Node> avoid, Hero hero, Boolean hasEnemy) {
        Weapon gunException = PathFinderService.getNearestGun(gameMap, player, "");
        Weapon meleeException = PathFinderService.getNearestWeapon(gameMap, player, "");
        Weapon nearestGun = PathFinderService.getNearestGun(gameMap, player, "");
        Obstacle nearestChest = PathFinderService.getNearestChest(gameMap, player);
        Armor nearestArmor = PathFinderService.getNearestArmor(gameMap, player, new ArrayList<>());
        Weapon nearestWeapon = PathFinderService.getNearestWeapon(gameMap, player, "");
        SupportItem nearestSupportItem = PathFinderService.getNearestSupportItem(gameMap, player);
        Player nearestEnemy = PathFinderService.getNearestOtherPlayer(gameMap, player);

        int distanceToNearestGun = PathFinderService.getDistanceToNearestGun(gameMap, player, "", avoid);
        int distanceToNearestChest = PathFinderService.getDistanceToNearestChest(gameMap, player);
        int distanceToNearestArmor = PathFinderService.getDistanceToNearestArmor(gameMap, player, new ArrayList<>(), avoid);
        int distanceToNearestEnemy = PathFinderService.getDistanceToNearestOtherPlayer(gameMap, player);
        int distanceToNearestWeapon = PathFinderService.getDistanceToNearestWeapon(gameMap, player, "", avoid);
        int distanceToSupportItem = PathFinderService.getDistanceToSupportItem(gameMap,player);

        if(meleeException != null){
            if(!hero.getInventory().getMelee().getId().equals("HAND") && meleeException.getId().equals("TREE_BRANCH")){
                nearestWeapon = PathFinderService.getNearestWeapon(gameMap, player, meleeException.getId());
                distanceToNearestWeapon = PathFinderService.getDistanceToNearestWeapon(gameMap, player, meleeException.getId(), avoid);
            }
        }
        if(gunException != null){
            if(hero.getInventory().getGun() != null && gunException.getId().equals("RUBBER_GUN")){
                nearestGun = PathFinderService.getNearestGun(gameMap, player, gunException.getId());
                distanceToNearestGun = PathFinderService.getDistanceToNearestGun(gameMap, player, gunException.getId(), avoid);
            }
        }

        if((hero.getInventory().getArmor() != null && hero.getInventory().getArmor().getId().equals("MAGIC_ARMOR")) || (hero.getInventory().getHelmet() != null && hero.getInventory().getHelmet().getId().equals("MAGIC_HELMET"))) {

            ArrayList<String> armorHelmetExceptions = new ArrayList<>();
            if(hero.getInventory().getArmor() != null && hero.getInventory().getArmor().getId().equals("MAGIC_ARMOR")) {
                armorHelmetExceptions.add("ARMOR");
            }
            if(hero.getInventory().getHelmet() != null && hero.getInventory().getHelmet().getId().equals("MAGIC_HELMET")) {
                armorHelmetExceptions.add("WOODEN_HELMET");
            }
            nearestArmor = PathFinderService.getNearestArmor(gameMap, player, armorHelmetExceptions);
            distanceToNearestArmor =  PathFinderService.getDistanceToNearestArmor(gameMap, player, armorHelmetExceptions, avoid);
        }

        PriorityTask gunTask = new PriorityTask(TaskType.GUN, 5, distanceToNearestGun, nearestGun);
        PriorityTask enemyTask = new PriorityTask(TaskType.ENEMY, 10, distanceToNearestEnemy, nearestEnemy);
        PriorityTask armorTask = new PriorityTask(TaskType.ARMOR, 4, distanceToNearestArmor, nearestArmor);
        PriorityTask chestTask = new PriorityTask(TaskType.CHEST, 4, distanceToNearestChest, nearestChest);
        PriorityTask weaponTask = new PriorityTask(TaskType.WEAPON, 4, distanceToNearestWeapon, nearestWeapon);
        PriorityTask supportItemTask = new PriorityTask(TaskType.SUPPORT_ITEM, 4, distanceToSupportItem, nearestSupportItem);

        List<PriorityTask> tasks = new ArrayList<>();
        tasks.add(gunTask);
        tasks.add(chestTask);
        tasks.add(armorTask);
        tasks.add(weaponTask);
        tasks.add(supportItemTask);

        if(gameMap.getStepNumber() >= timeAvoid && hero.getInventory().getGun() == null && hero.getInventory().getMelee().equals("HAND")){ //1080
            PriorityTask runTask = new PriorityTask(TaskType.RUN, 15, distanceToNearestEnemy, new Element());
            tasks.add(runTask);
        }

        if (hasEnemy) tasks.add(enemyTask);
        tasks.sort(new Comparator<PriorityTask>() {
            @Override
            public int compare(PriorityTask o1, PriorityTask o2) {
                if (o1.distanceToPlayer == o2.distanceToPlayer) {
                    return o2.priorityPoint - o1.priorityPoint;
                }
                return o1.distanceToPlayer - o2.distanceToPlayer;
            }
        });
        System.out.println("TASK LIST AFTER SORT:");
        for(PriorityTask p : tasks) {
            System.out.println(p.name.name());
        }
        Inventory inventory = hero.getInventory();
        // Có súng và có địch -> ưu tiên cao nhất

        if (hasEnemy && inventory.getGun() != null && distanceToNearestEnemy <= hero.getInventory().getGun().getRange()[1] / 1.5f && gameMap.getStepNumber() >= timeAvoid) { //1080
            return enemyTask;
        }
        else if(hasEnemy && inventory.getGun() != null && distanceToNearestEnemy <= hero.getInventory().getGun().getRange()[1] * 1.5f && gameMap.getStepNumber() < timeAvoid){
            return enemyTask;
        }
        // Không thì cứ làm việc gần nhất có thể để kiếm điểm
        else {
            return tasks.getFirst();
        }

    }

    private void searchForGun(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        String path = PathFinderService.findPathToNearestGun(gameMap, player, avoid, "");
        if (path == null)
        {
            return;
        }
        if (path.isEmpty()){
            swapItem(gameMap, player, hero);
            isPickupGun = true;
        }
        else {
            hero.move(path);
        }
    }

    private void searchForGun(GameMap gameMap, Player player, List<Node> avoid, Node gunTarget) throws IOException {
//        String path = PathFinderService.findPathToNearestGun(gameMap, player, avoid, gunException);
        String path = PathUtils.getShortestPath(gameMap, avoid, player, gunTarget, true);
        if (path == null)
        {
            return;
        }
        if (path.isEmpty()){
            swapItem(gameMap, player, hero);
            isPickupGun = true;
        }
        else {
            hero.move(path);
        }
    }

    private void searchForSupportItem(GameMap gameMap, Player player, List<Node> avoid, Element element) throws IOException {
//        String path = PathFinderService.findPathToSupportItem(gameMap, player, avoid);
        String path = PathUtils.getShortestPath(gameMap, avoid, player, element, true);
        if (path == null) return;
        System.out.println(element + path);
        if (path.isEmpty()) {
            swapItem(gameMap, player, hero);
        } else {
            hero.move(path);
        }
    }

    public boolean checkSingleDirection(String path) {
        return PathFinderService.checkSingleDirection(path);
    }

    private void searchForEnemy(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        String path = PathFinderService.findPathToNearestOtherPlayer(gameMap, player, avoid);
        if (path == null) return;
        int distanceToEnemy = PathFinderService.getDistanceToNearestOtherPlayer(gameMap, player);
        String direction = path.charAt(path.length()-1) + "";

        if(hero.getInventory().getThrowable() != null && hero.getInventory().getThrowable().getRange()[1] == distanceToEnemy && checkSingleDirection(path)) {
            hero.throwItem(direction);
            } else  if(hero.getInventory().getGun() != null && hero.getInventory().getGun().getRange()[1] >=  distanceToEnemy && checkSingleDirection(path))
            hero.shoot(direction);
            else if(distanceToEnemy == 1)
                hero.attack(direction);
         else {
            hero.move(path);
        }
    }

    private void  swapItem(GameMap gameMap, Player player, Hero hero) throws IOException {
        Element element = gameMap.getElementByIndex(player.getX(), player.getY());
        ElementType type = element.getType();
        System.out.println("GOT TYPE: " + type.name());
        System.out.println("ELEMENT FOUND: " + element.getId() + " / TYPE: " + element.getType());
//        Weapon weapon = null;
//        if (type == ElementType.GUN) {
//            weapon = (Weapon) element;
//        }

        Inventory inventory = hero.getInventory();

        if (type.name().equals(ElementType.MELEE.name()) && !inventory.getMelee().getId().equals("HAND")) {
            System.out.println("DROP MELEE");
            hero.revokeItem(inventory.getMelee().getId());
        } else
        if (type.name().equals(ElementType.GUN.name()) && inventory.getGun() != null ) {
            System.out.println("DROP GUN");
            hero.revokeItem(inventory.getGun().getId());
        }
        else if (type.name().equals(ElementType.THROWABLE.name()) && inventory.getThrowable() != null) {
            hero.revokeItem(inventory.getThrowable().getId());
        }
        else if (type.name().equals(ElementType.SPECIAL.name()) && inventory.getSpecial() != null) {
            System.out.println("DROP SPECIAL");

            hero.revokeItem(inventory.getSpecial().getId());
        }
        else if (type.name().equals(ElementType.ARMOR.name()) && inventory.getArmor() != null) {
            System.out.println("DROP ARMOR");
            Armor armor = (Armor) element;hero.revokeItem(inventory.getArmor().getId());
        }
        else if (type.name().equals(ElementType.HELMET.name()) && (inventory.getHelmet() != null )) {
            System.out.println("DROP HELMET");
            System.out.println(inventory.getHelmet());
            Armor armor = (Armor) element;
            // Mũ giảm nhiều dame hơn, hoặc giảm cùng dame nhưng nhiều máu hơn -> Lấy
            hero.revokeItem(inventory.getHelmet().getId());
        }
        else if (type.name().equals(ElementType.SUPPORT_ITEM.name()) && sizeOfSupportItem(gameMap,player) == 4) {
            SupportItem shouldUse = null;
            int min_heal = Integer.MAX_VALUE;
           for(SupportItem si : inventory.getListSupportItem()) {
                if(si.getHealingHP() < min_heal) {
                    min_heal = si.getHealingHP();
                    shouldUse = si;
                }
           }
            System.out.println(shouldUse);
           if (shouldUse != null) {
               hero.useItem(shouldUse.getId());
           }
        }
        else {
            System.out.println("pick up item");
            System.out.println(element + " "  + type);
            hero.pickupItem();
        }
    }
    private int sizeOfSupportItem(GameMap gameMap, Player player) throws IOException {
        List<SupportItem> supportItems = hero.getInventory().getListSupportItem();
    int size =  0;
    for(SupportItem si : supportItems) {
        size++;
    }
    return size;
    }
    private void searchForChestAndLoot(GameMap gameMap, Player player, List<Node> avoid, Element nearestChest) throws IOException {
//        String path = PathFinderService.findPathToNearestChest(gameMap, player, avoid);
          String path = PathUtils.getShortestPath(gameMap, avoid, player, nearestChest, true);
        if (path == null) return;
        int distanceToChest = PathFinderService.getDistanceToNearestChest(gameMap, player);
        if (distanceToChest == 1) {
            hero.attack(path.charAt(path.length() - 1) + "");
        }
        else {
            hero.move(path);
        }
    }

    private void searchForArmorOrHelmet(GameMap gameMap, Player player, List<Node> avoid, Element element) throws IOException {
//        String path = PathFinderService.findPathToNearestArmor(gameMap, player, avoid, exceptions);
        String path = PathUtils.getShortestPath(gameMap, avoid, player, element, true);
        if (path == null) return;

        if (path.isEmpty()) {
            swapItem(gameMap, player, hero);
        } else {
            hero.move(path);
        }
    }

    private void searchForWeapon(GameMap gameMap, Player player, List<Node> avoid, Element element) throws IOException {
//        String path = PathFinderService.findPathToNearestWeapon(gameMap, player, avoid);
        String path = PathUtils.getShortestPath(gameMap, avoid, player, element, true);
        if (path == null) return;

        int distanceToWeapon = PathFinderService.getDistanceToNearestWeapon(gameMap, player, "", avoid);
        System.out.println("Path to weapon " + path + "/Distance to weapon" + distanceToWeapon);
        if (path.isEmpty()) {
            swapItem(gameMap, player, hero);
            isPickupMelee = true;
        } else {
            hero.move(path);
        }
    }

    private boolean selectSupportItems(GameMap gameMap, Hero hero, Player player, List<Node> avoid) throws IOException {
        //
        if (hero.getInventory().getThrowable() != null && hero.getInventory().getThrowable().getId().equals("SMOKE")) {
            hero.revokeItem(hero.getInventory().getThrowable().getId());
            return true;
        }
        List<SupportItem> supportItems = hero.getInventory().getListSupportItem();
        if (supportItems.isEmpty()) return false;
        System.out.println(supportItems.size());
        for (SupportItem si : supportItems) {
            if (si.getId().equals("GOD_LEAF") || si.getId().equals("SPIRIT_TEAR") || si.getId().equals("MERMAID_TAIL")) {
                if (player.getHealth() + si.getHealingHP() <= 100) {
                    hero.useItem(si.getId());
                    return true;
                }
            } else if (si.getId().equals("UNICORN_BLOOD") || si.getId().equals("PHOENIX_FEATHERS")) {
                if (player.getHealth() <= 60) {
                    hero.useItem(si.getId());
                    return true;
                }
            } else {
                int distanceToEnemy = PathFinderService.getDistanceToNearestOtherPlayer(gameMap, player);
                if (si.getId().equals("COMPASS")) {
                    if (distanceToEnemy <= 4) {
                        hero.useItem(si.getId());
                        return true;
                    }
                } else {
                    hero.useItem(si.getId());
                    return true;
                }
            }
        }
        return false;

    }

    private void searchForEnemy_v2(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        System.out.println("NEXT_ACTION: " + nextAction);
//        if(nextAction.equals("404")) return;
        List<Node> avoidChest = NodeAvoidanceUtil.computeNodesToAvoid(gameMap);
        avoidChest.addAll(gameMap.getObstaclesByTag("DESTRUCTIBLE"));
        String path = PathFinderService.findPathToNearestOtherPlayer(gameMap, player, avoidChest);
        if (path == null || path.isEmpty()) return;

        String pathNext = "";
        if(!nextAction.equals("404")){
            if((path.charAt(path.length()-1) + "").equals("l") && nextAction.equals("r")) {
                pathNext = path.substring(0, path.length() - 1);
            } else if((path.charAt(path.length()-1) + "").equals("r") && nextAction.equals("l")) {
                pathNext = path.substring(0, path.length() - 1);
            } else if((path.charAt(path.length()-1) + "").equals("d") && nextAction.equals("u")) {
                pathNext = path.substring(0, path.length() - 1);
            } else if((path.charAt(path.length()-1) + "").equals("u") && nextAction.equals("d")) {
                pathNext = path.substring(0, path.length() - 1);
            } else{
                pathNext = path + nextAction;
            }
        }

        /////
        Player enemy = PathFinderService.getNearestOtherPlayer(gameMap, player);

        int distanceToEnemy = PathFinderService.getDistanceToNearestOtherPlayer(gameMap, player);

        if(nextAction.equals("r")) enemy.x += 1;
        else if(nextAction.equals("l")) enemy.x -= 1;
        else if(nextAction.equals("u")) enemy.y += 1;
        else if(nextAction.equals("d")) enemy.y -= 1;

        int distanceToEnemyNext = distance(player, enemy);

        String direction = path.charAt(path.length()-1) + "";
        String directionNext;
        if(pathNext.isEmpty()){
            directionNext = "r";
        }
        else{
            directionNext = pathNext.charAt(pathNext.length()-1) + "";
        }
        System.out.println(direction + " " + directionNext);
         if((hero.getInventory().getThrowable() != null ) && checkDirectionThrow(gameMap, player, avoid,  hero.getInventory().getThrowable())){
            hero.throwItem(direction);
        }
        if(hero.getInventory().getSpecial() != null && (coolDownSpecialReamin == 0f)   && ((hero.getInventory().getSpecial().getId().equals("ROPE") && distanceToEnemy <=6 &&(checkSingleDirection(path))) || (hero.getInventory().getSpecial().getId().equals("SAHUR_BAT") && distanceToEnemy <= 5 &&(checkSingleDirection(path))) ||(hero.getInventory().getSpecial().getId().equals("BELL") && distanceToEnemy <= 3.5 ))){
            Weapon speacialItem = hero.getInventory().getSpecial();
            coolDownSpecialReamin =  speacialItem.getCooldown();
            usedSpecialItems = true;
            hero.useSpecial(direction);
            System.out.println("USE SPEACIAL : " + direction);
        }
        else if((hero.getInventory().getGun() != null && hero.getInventory().getGun().getRange()[1] >= distanceToEnemy && checkSingleDirection(path)) && canShoot){
            hero.shoot(direction);
            counter_cooldown_gun = 0;
            canShoot = false;
            System.out.println("SHOOTING ENEMY: " + direction);
        }

        else if((distanceToEnemy == hero.getInventory().getMelee().getRange()[1]) && canAttack){
            hero.attack(direction);
            counter_cooldown_melee = 0;
            canAttack = false;
            System.out.println("ATTACKING ENEMY: " + direction);
        }
        else if(hero.getInventory().getThrowable() != null  && checkDirectionThrow(gameMap, player, avoid, hero.getInventory().getThrowable()) && !nextAction.equals("404")) {
            hero.throwItem(directionNext);
        }
        else if(hero.getInventory().getGun() != null && hero.getInventory().getGun().getRange()[1] >= distanceToEnemyNext && checkSingleDirection(pathNext) && canShoot && !nextAction.equals("404")) {
            hero.shoot(directionNext);
            counter_cooldown_gun = 0;
            canShoot = false;
            System.out.println("SHOOTING ENEMY: " + directionNext);
        }
        else if(distanceToEnemyNext == 1 && canAttack && !nextAction.equals("404")) {
            hero.attack(directionNext);
            counter_cooldown_melee = 0;
            canAttack = false;
            System.out.println("ATTACKING ENEMY: " + directionNext);
        }
        else{
            System.out.println("Move to enemy: " + path);
            if(path.length() == 1 && !canAttack){
                // FIX SAU
                if(direction.equals("d") || direction.equals("u")){
                    hero.move("r");
                }
                else{
                    hero.move("u");
                }
            }
            else{
                hero.move(path);
            }
//            hero.move(path);
        }
    }

    private boolean checkDirectionThrow(GameMap gameMap, Player player, List<Node> avoid, Weapon weapon){
    Player enemy = PathFinderService.getNearestOtherPlayer(gameMap, player);
    if(enemy.getX() <=  player.getX() + weapon.getExplodeRange()/2 + weapon.getRange()[1] && enemy.getX() >= player.getX() - weapon.getExplodeRange()/2 +  weapon.getRange()[1] && enemy.getY() <= player.getY() + weapon.getExplodeRange()/2 && enemy.getY() >= player.getY() - weapon.getExplodeRange()/2) return true;
    if(enemy.getY() <=  player.getY() + weapon.getExplodeRange()/2 +  weapon.getRange()[1] && enemy.getY() >= player.getY() - weapon.getExplodeRange()/2 +  weapon.getRange()[1] && enemy.getX() <=  player.getX() + weapon.getExplodeRange()/2  && enemy.getX() >= player.getX() - weapon.getExplodeRange()/2  ) return true;
    return false;
}
    private void trackingNearestPlayer(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Player enemy = PathFinderService.getNearestOtherPlayer(gameMap, player);
        boolean isNewEnemy = false;

        if(enemy == null) return;
        if (enemyTracked == null || !enemyTracked.getId().equals(enemy.getId())) {
            enemy_pathToGun = "";
            enemy_pathToWeapon = "";
            enemy_pathToSupportItem = "";
            enemy_pathToChest = "";
            enemy_pathToArmor = "";
            enemy_pathToPlayer = "";
            nextAction = "";
            countEnemyAction_toGun = 0;
            countEnemyAction_toWeapon = 0;
            countEnemyAction_toSupportItem = 0;
            countEnemyAction_toChest = 0;
            countEnemyAction_toArmor = 0;
            countEnemyAction_toPlayer = 0;
            oldNodeX = enemy.getX();
            oldNodeY = enemy.getY();
            counterActionIdle = 0;
            enemyTracked = enemy;

            isNewEnemy = true;
        }

        String oldAction = getOldActionNearestEnemy(enemy);

        if(!isNewEnemy){
            if(nextAction.equals(oldAction)){
                correctedAction++;
            }
            System.out.println("--------------------------------------------------");
            System.out.println("Corrected action: " + correctedAction + " | step: " + gameMap.getStepNumber() + " | percent: " + (correctedAction * 100 / gameMap.getStepNumber()) + "%");
            System.out.println("--------------------------------------------------");
        }

        if (oldAction.isEmpty()) {
            nextAction = "";
        }

        if (enemy_pathToGun != null) {
            if(!enemy_pathToGun.isEmpty()){
                if((enemy_pathToGun.charAt(0) + "").equals(oldAction)) {
//                    countEnemyAction_toGun++;
                    countEnemyAction_toGun += ((float) 1 /enemy_pathToGun.length());
                } else {
                    countEnemyAction_toGun = 0;
//                    else countEnemyAction_toGun -= 1;
                }
            }
//            System.out.println("Enemy path to gun: " + enemy_pathToGun + " | Count: " + countEnemyAction_toGun);
        }

        if(enemy_pathToWeapon != null) {
            if(!enemy_pathToWeapon.isEmpty()){
                if((enemy_pathToWeapon.charAt(0) + "").equals(oldAction)) {
//                    countEnemyAction_toWeapon++;
                    countEnemyAction_toWeapon += ((float) 1 / enemy_pathToWeapon.length());
                } else {
                    countEnemyAction_toWeapon = 0;
//                    else countEnemyAction_toWeapon -= 1;
                }
            }
//            System.out.println("Enemy path to weapon: " + enemy_pathToWeapon + " | Count: " + countEnemyAction_toWeapon);
        }
        if(enemy_pathToSupportItem != null) {
            if(!enemy_pathToSupportItem.isEmpty()){
                if((enemy_pathToSupportItem.charAt(0) + "").equals(oldAction)) {
//                    countEnemyAction_toSupportItem++;
                    countEnemyAction_toSupportItem += ((float) 1 / enemy_pathToSupportItem.length());
                } else {
                    countEnemyAction_toSupportItem = 0;
//                    else countEnemyAction_toSupportItem -= 1;
                }
            }
//            System.out.println("Enemy path to support item: " + enemy_pathToSupportItem + " | Count: " + countEnemyAction_toSupportItem);
        }

        if(enemy_pathToChest != null) {
            if(!enemy_pathToChest.isEmpty()){
                if((enemy_pathToChest.charAt(0) + "").equals(oldAction)) {
//                    countEnemyAction_toChest++;
                    countEnemyAction_toChest += ((float) 1 / enemy_pathToChest.length());
                } else {
                    countEnemyAction_toChest = 0;
//                    else countEnemyAction_toChest -= 1;
                }
            }
//            System.out.println("Enemy path to chest: " + enemy_pathToChest + " | Count: " + countEnemyAction_toChest);
        }

        if(enemy_pathToArmor != null) {
            if(!enemy_pathToArmor.isEmpty()){
                if((enemy_pathToArmor.charAt(0) + "").equals(oldAction)) {
//                    countEnemyAction_toArmor++;
                    countEnemyAction_toArmor += ((float) 1 / enemy_pathToArmor.length());
                } else {
                    countEnemyAction_toArmor = 0;
//                    else countEnemyAction_toArmor -= 1;
                }
            }
//            System.out.println("Enemy path to armor: " + enemy_pathToArmor + " | Count: " + countEnemyAction_toArmor);
        }

        if(enemy_pathToPlayer != null) {
            if(!enemy_pathToPlayer.isEmpty()){
                if((enemy_pathToPlayer.charAt(0) + "").equals(oldAction)) {
//                    countEnemyAction_toPlayer++;
                    countEnemyAction_toPlayer += ((float) 1 / enemy_pathToPlayer.length());
                } else {
                    countEnemyAction_toPlayer = 0;
//                    else countEnemyAction_toPlayer -= 1;
                }
            }
//            System.out.println("Enemy path to player: " + enemy_pathToPlayer + " | Count: " + countEnemyAction_toPlayer);
        }

        //Update data

        enemy_pathToGun = PathFinderService.findPathToNearestGun(gameMap, enemy, avoid, "");
        enemy_pathToWeapon = PathFinderService.findPathToNearestWeapon(gameMap, enemy, avoid, "");
        enemy_pathToSupportItem = PathFinderService.findPathToSupportItem(gameMap, enemy, avoid);
        enemy_pathToChest = PathFinderService.findPathToNearestChest(gameMap, enemy, avoid);
        enemy_pathToArmor = PathFinderService.findPathToNearestArmor(gameMap, enemy, avoid, new ArrayList<>());
//        enemy_pathToPlayer = reversePathToPlayer(PathFinderService.findPathToNearestOtherPlayer(gameMap, player, avoid));
        enemy_pathToPlayer = PathUtils.getShortestPath(gameMap, avoid, enemy, player, false);

        oldNodeX = enemy.getX();
        oldNodeY = enemy.getY();

        //Processing

        if(isNewEnemy){
            System.out.println("IsNewEnemy, resetting action counts.");
            nextAction = "404";
            return;
        }

        if(oldAction.isEmpty()) {
            System.out.println("Old action is empty, skipping action selection.");
            System.out.println("Time: " + gameMap.getStepNumber() + " | Old Action: " + oldAction + " | Next Action: " + nextAction);
            System.out.println();
            nextAction = "";
            counterActionIdle++;
            return;
        }

        float maxCount = Math.max(Math.max(Math.max(Math.max(countEnemyAction_toGun, countEnemyAction_toWeapon), countEnemyAction_toSupportItem),
                Math.max(countEnemyAction_toChest, countEnemyAction_toArmor)), countEnemyAction_toPlayer);

        // Kiểm tra có nhiều count bằng nhau
        List<String> actionsWithMaxCount = new ArrayList<>();
        if (Math.abs(countEnemyAction_toGun - maxCount) <= 0.001 && enemy_pathToGun != null){
            if(enemy_pathToGun.isEmpty()){
                actionsWithMaxCount.add("");
            }
            else actionsWithMaxCount.add(enemy_pathToGun.charAt(0) + "");
        }
        else{
//            System.out.println("Count to gun: " + countEnemyAction_toGun + " | Count: " + countEnemyAction_toGun);
        }
        if (Math.abs(countEnemyAction_toWeapon - maxCount) <= 0.001 && enemy_pathToWeapon != null){
            if(enemy_pathToWeapon.isEmpty()){
                actionsWithMaxCount.add("");
            }
            else actionsWithMaxCount.add(enemy_pathToWeapon.charAt(0) + "");
        }
        else{
//            System.out.println("Count to weapon: " + countEnemyAction_toWeapon + " | Count: " + countEnemyAction_toWeapon);
        }

        if (Math.abs(countEnemyAction_toSupportItem - maxCount) <= 0.001 && enemy_pathToSupportItem != null){
            if(enemy_pathToSupportItem.isEmpty()){
                actionsWithMaxCount.add("");
            }
            else actionsWithMaxCount.add(enemy_pathToSupportItem.charAt(0) + "");
        }
        else{
//            System.out.println("Count to support item: " + countEnemyAction_toSupportItem + " | Count: " + countEnemyAction_toSupportItem);
        }

        if (Math.abs(countEnemyAction_toChest - maxCount) <= 0.001 && enemy_pathToChest != null) {
            if (enemy_pathToChest.isEmpty()) {
                actionsWithMaxCount.add("");
            } else actionsWithMaxCount.add(enemy_pathToChest.charAt(0) + "");
        }
        else{
//            System.out.println("Count to chest: " + countEnemyAction_toChest + " | Count: " + countEnemyAction_toChest);
        }

        if (Math.abs(countEnemyAction_toArmor - maxCount) <= 0.001 && enemy_pathToArmor != null){
            if(enemy_pathToArmor.isEmpty()){
                actionsWithMaxCount.add("");
            }
            else actionsWithMaxCount.add(enemy_pathToArmor.charAt(0) + "");
        }
        else{
//            System.out.println("Count to armor: " + countEnemyAction_toArmor + " | Count: " + countEnemyAction_toArmor);
        }

        if (Math.abs(countEnemyAction_toPlayer - maxCount) <= 0.001 && enemy_pathToPlayer != null){
            if(enemy_pathToPlayer.isEmpty()){
                actionsWithMaxCount.add("");
            }
            else actionsWithMaxCount.add(enemy_pathToPlayer.charAt(0) + "");
        }
        else{
//            System.out.println("Count to player: " + countEnemyAction_toPlayer + " | Count: " + countEnemyAction_toPlayer);
        }

        if (actionsWithMaxCount.size() > 1) {
            Map<String, Long> frequencyMap = actionsWithMaxCount.stream()
                    .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

            String majorityAction = frequencyMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();

            nextAction = majorityAction;
        }
        else if(actionsWithMaxCount.isEmpty()) {
            nextAction = "404";
        }
        else {
            if( actionsWithMaxCount.get(0).isEmpty()) {
                nextAction = "";
            }
            else nextAction = actionsWithMaxCount.get(0);
        }

        System.out.println("Time: " + gameMap.getStepNumber() + " | Old Action: " + oldAction + " | Next Action: " + nextAction);
        System.out.println();
    }
    private String getOldActionNearestEnemy(Player enemy){
        if(oldNodeX == enemy.getX() && oldNodeY == enemy.getY()) {
            return "";
        }
        else if( enemy.getX() > oldNodeX) {
            return "r";
        } else if (enemy.getX() < oldNodeX) {
            return "l";
        } else if (enemy.getY() > oldNodeY) {
            return "u";
        } else {
            return "d";
        }
    }
    public void updateOtherPlayerPosition(GameMap gameMap,Player player, Map<String, Node> mp){
        List<Player> p = gameMap.getOtherPlayerInfo();
        for(Player pl : p){
            mp.put(pl.getID(), pl.getPosition());
        }
    }

    // Hàm tập trung né người + bo
    public String reverseString(String s) {
        return new StringBuilder(s).reverse().toString();
    }
    public void avoidOtherPlayer_v1(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        boolean isInSafe = PathUtils.checkInsideSafeArea(player,  gameMap.getSafeZone(), gameMap.getMapSize());

        String path = PathFinderService.findPathToNearestOtherPlayer(gameMap, player, avoid);
        if (path == null || path.isEmpty()) return;
        if(isInSafe){
            String reversePath = reverseString(path);
            hero.move(reversePath);
        }
        else{
            int mapSize = gameMap.getMapSize();
            int safeZone = gameMap.getSafeZone();
            int centerX = mapSize / 2;
            int centerY = mapSize / 2;

            int dx = centerX - player.getX();
            int dy = centerY - player.getY();

            // Ưu tiên hướng theo khoảng cách lớn hơn (đi gần tâm bo hơn)
            String moveDir = "r";
            if (Math.abs(dx) >= Math.abs(dy)) {
                moveDir = dx > 0 ? "r" : "l";
            } else {
                moveDir = dy > 0 ? "d" : "u";
            }
            hero.move(String.valueOf(moveDir));
        }
    }
    private boolean CheckContainAvoid(List<Node> avoid, Node x){
        for(Node node : avoid){
            if(x.getX() == node.getX() && x.getY() == node.getY()) return true;
        }
        return  false;
    }

    public void avoidOtherPlayer_v2(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        System.out.println("RUN!!!!");
        String path = PathFinderService.findPathToNearestOtherPlayer(gameMap, player, avoid);
        Player enemy = PathFinderService.getNearestOtherPlayer(gameMap, player);
        if (path == null || path.isEmpty()) return;

        Node leftNode = new Node(player.getX() - 1, player.getY());
        Node rightNode = new Node(player.getX() + 1, player.getY());
        Node upNode = new Node(player.getX(), player.getY() + 1);
        Node downNode = new Node(player.getX(), player.getY() - 1);

        double leftScore = 0;
        double rightScore = 0;
        double upScore = 0;
        double downScore = 0;

        if(!CheckContainAvoid(avoid, leftNode) && PathUtils.checkInsideSafeArea(leftNode, gameMap.getSafeZone(), gameMap.getMapSize())) {
            leftScore = Math.sqrt(Math.pow(leftNode.x - enemy.getX(), 2) + Math.pow(leftNode.y - enemy.getY(), 2));
        }
        if(!CheckContainAvoid(avoid, rightNode) && PathUtils.checkInsideSafeArea(rightNode, gameMap.getSafeZone(), gameMap.getMapSize())){
            rightScore = Math.sqrt(Math.pow(rightNode.x - enemy.getX(), 2) + Math.pow(rightNode.y - enemy.getY(), 2));
        }
        if(!CheckContainAvoid(avoid, upNode) && PathUtils.checkInsideSafeArea(upNode, gameMap.getSafeZone(), gameMap.getMapSize())) {
            upScore = Math.sqrt(Math.pow(upNode.x - enemy.getX(), 2) + Math.pow(upNode.y - enemy.getY(), 2));
        }
        if(!CheckContainAvoid(avoid, downNode) && PathUtils.checkInsideSafeArea(downNode, gameMap.getSafeZone(), gameMap.getMapSize())) {
            downScore = Math.sqrt(Math.pow(downNode.x - enemy.getX(), 2) + Math.pow(downNode.y - enemy.getY(), 2));
        }

        double maxScore = Math.max(Math.max(leftScore, rightScore), Math.max(upScore, downScore));

        if(Math.abs(maxScore - leftScore) <= 0.01 && !CheckContainAvoid(avoid, leftNode) && PathUtils.checkInsideSafeArea(leftNode, gameMap.getSafeZone(), gameMap.getMapSize())) {
            hero.move("l");
        } else if(Math.abs(maxScore - rightScore) <= 0.01 && !CheckContainAvoid(avoid, rightNode) && PathUtils.checkInsideSafeArea(rightNode, gameMap.getSafeZone(), gameMap.getMapSize())) {
            hero.move("r");
        } else if(Math.abs(maxScore - upScore) <= 0.01 && !CheckContainAvoid(avoid, upNode) && PathUtils.checkInsideSafeArea(upNode, gameMap.getSafeZone(), gameMap.getMapSize())) {
            hero.move("u");
        } else if(Math.abs(maxScore - downScore) <= 0.01 && !CheckContainAvoid(avoid, downNode) && PathUtils.checkInsideSafeArea(downNode, gameMap.getSafeZone(), gameMap.getMapSize())) {
            hero.move("d");
        } else {
            System.out.println("No valid move found, staying in place.");
        }
    }

    // tính theo đường đi
    public void avoidOtherPlayer_ShorestPath(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        System.out.println("RUN!!!!");
        String path = PathFinderService.findPathToNearestOtherPlayer(gameMap, player, avoid);
        Player enemy = PathFinderService.getNearestOtherPlayer(gameMap, player);
        if(enemy == null) return;;
        if (path == null || path.isEmpty()) return;

        Node leftNode = new Node(player.getX() - 1, player.getY());
        Node rightNode = new Node(player.getX() + 1, player.getY());
        Node upNode = new Node(player.getX(), player.getY() + 1);
        Node downNode = new Node(player.getX(), player.getY() - 1);

        double leftScore = 0;
        double rightScore = 0;
        double upScore = 0;
        double downScore = 0;

        if(!CheckContainAvoid(avoid, leftNode)) {
            leftScore = PathUtils.getShortestPath(gameMap, avoid, enemy, leftNode, true).length();
//            leftScore = PathFinderService.findPathToNearestOtherPlayer(gameMap, (Player)(leftNode), avoid).length();
        }

        if(!CheckContainAvoid(avoid, rightNode)){
            rightScore = PathUtils.getShortestPath(gameMap, avoid, enemy, rightNode, true).length();
//            rightScore = PathFinderService.findPathToNearestOtherPlayer(gameMap, (Player)(rightNode), avoid).length();
        }
        if(!CheckContainAvoid(avoid, upNode)) {
            upScore = PathUtils.getShortestPath(gameMap, avoid, enemy, upNode, true).length();
//            upScore = PathFinderService.findPathToNearestOtherPlayer(gameMap, (Player)(upNode), avoid).length();
        }
        if(!CheckContainAvoid(avoid, downNode)) {
            downScore = PathUtils.getShortestPath(gameMap, avoid, enemy, downNode, true).length();
//            downScore = PathFinderService.findPathToNearestOtherPlayer(gameMap, (Player)(upNode), avoid).length();
        }

        double maxScore = Math.max(Math.max(leftScore, rightScore), Math.max(upScore, downScore));

        if(Math.abs(maxScore - leftScore) <= 0.01 && !CheckContainAvoid(avoid, leftNode)) {
            hero.move("l");
        } else if(Math.abs(maxScore - rightScore) <= 0.01 && !CheckContainAvoid(avoid, rightNode)) {
            hero.move("r");
        } else if(Math.abs(maxScore - upScore) <= 0.01 && !CheckContainAvoid(avoid, upNode)) {
            hero.move("u");
        } else if(Math.abs(maxScore - downScore) <= 0.01 && !CheckContainAvoid(avoid, downNode)) {
            hero.move("d");
        } else {
            System.out.println("No valid move found, staying in place.");
        }
    }

    public void avoidOtherPlayer_All(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        System.out.println("RUN!!!!");
        String path = PathFinderService.findPathToNearestOtherPlayer(gameMap, player, avoid);
        Player enemy = PathFinderService.getNearestOtherPlayer(gameMap, player);
        if (path == null || path.isEmpty()) return;

        Node leftNode = new Node(player.getX() - 1, player.getY());
        Node rightNode = new Node(player.getX() + 1, player.getY());
        Node upNode = new Node(player.getX(), player.getY() + 1);
        Node downNode = new Node(player.getX(), player.getY() - 1);

        double leftScore = 0;
        double rightScore = 0;
        double upScore = 0;
        double downScore = 0;
        List<Player> list = gameMap.getOtherPlayerInfo();
        for (Player p : list ) {
            Node currentPlayerPosition = p.getPosition();
            if(!avoid.contains(leftNode)) {
                leftScore += Math.sqrt(Math.pow(leftNode.x - currentPlayerPosition.x, 2) + Math.pow(leftNode.y - currentPlayerPosition.y, 2));
            }
            if(!avoid.contains(rightNode)){
                rightScore += Math.sqrt(Math.pow(rightNode.x - currentPlayerPosition.x, 2) + Math.pow(rightNode.y - currentPlayerPosition.y, 2));
            }
            if(!avoid.contains(upNode)) {
                upScore += Math.sqrt(Math.pow(upNode.x - currentPlayerPosition.x, 2) + Math.pow(upNode.y - currentPlayerPosition.y, 2));
            }
            if(!avoid.contains(downNode)) {
                downScore += Math.sqrt(Math.pow(downNode.x - currentPlayerPosition.x, 2) + Math.pow(downNode.y - currentPlayerPosition.y, 2));
            }


        }

        if(!avoid.contains(leftNode)) {
            leftScore = PathFinderService.findPathToNearestOtherPlayer(gameMap, (Player)(leftNode), avoid).length();
        }
        if(!avoid.contains(rightNode)){
            rightScore = PathFinderService.findPathToNearestOtherPlayer(gameMap, (Player)(rightNode), avoid).length();
        }
        if(!avoid.contains(upNode)) {
            upScore = PathFinderService.findPathToNearestOtherPlayer(gameMap, (Player)(upNode), avoid).length();
        }
        if(!avoid.contains(downNode)) {
            downScore = PathFinderService.findPathToNearestOtherPlayer(gameMap, (Player)(upNode), avoid).length();
        }

        double maxScore = Math.max(Math.max(leftScore, rightScore), Math.max(upScore, downScore));

        if(Math.abs(maxScore - leftScore) <= 0.01 && !avoid.contains(leftNode)) {
            hero.move("l");
        } else if(Math.abs(maxScore - rightScore) <= 0.01 && !avoid.contains(rightNode)) {
            hero.move("r");
        } else if(Math.abs(maxScore - upScore) <= 0.01 && !avoid.contains(upNode)) {
            hero.move("u");
        } else if(Math.abs(maxScore - downScore) <= 0.01 && !avoid.contains(downNode)) {
            hero.move("d");
        } else {
            System.out.println("No valid move found, staying in place.");
        }
    }

    public void attackingStatus(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        System.out.println("-----------------------");
        if(hero.getInventory().getGun() != null){
            counter_cooldown_gun += 1;
            counter_cooldown_gun = Math.min(counter_cooldown_gun, cooldownTimer_gun);

            if(Math.abs(counter_cooldown_gun - cooldownTimer_gun) <= 0.01f){
                canShoot = true;
            }
            else{
                canShoot = false;
            }

            System.out.println("Counter cooldown gun: " + counter_cooldown_gun + " | Cooldown timer: " + cooldownTimer_gun);
        }
        else canShoot = false;

        if(hero.getInventory().getMelee() != null){
            counter_cooldown_melee += 1;
            counter_cooldown_melee = Math.min(counter_cooldown_melee, cooldownTimer_melee);

            if(Math.abs(counter_cooldown_melee - cooldownTimer_melee) <= 0.01f){
                canAttack = true;
            }
            else{
                canAttack = false;
            }
            System.out.println("Counter cooldown melee: " + counter_cooldown_melee + " | Cooldown timer: " + cooldownTimer_melee);
        }
        else canAttack = false;

        System.out.println("STATUS: " + canShoot + " " + canAttack);
    }
}
