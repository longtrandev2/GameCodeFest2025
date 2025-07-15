package services;

import jsclub.codefest.sdk.model.Element;

public class PriorityTask {
    public GameActionHandler.TaskType name;
    public int priorityPoint;
    public int distanceToPlayer;
    public Element element;

    public PriorityTask(GameActionHandler.TaskType name, int priorityPoint, int distanceToPlayer, Element element) {
        this.name = name;
        this.priorityPoint = priorityPoint;
        this.distanceToPlayer = distanceToPlayer;
        this.element = element;
    }
}
