package services;

public class PriorityTask {
    public GameActionHandler.TaskType name;
    public int priorityPoint;
    public int distanceToPlayer;

    public PriorityTask(GameActionHandler.TaskType name, int priorityPoint, int distanceToPlayer) {
        this.name = name;
        this.priorityPoint = priorityPoint;
        this.distanceToPlayer = distanceToPlayer;
    }
}
