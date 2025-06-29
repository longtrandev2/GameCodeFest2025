package services;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;

import java.util.ArrayList;
import java.util.List;

public class NodeAvoidanceUtil {
    public static List<Node> computeNodesToAvoid(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        nodes.addAll(gameMap.getObstaclesByTag("TRAP"));
        return nodes;
    }
}
