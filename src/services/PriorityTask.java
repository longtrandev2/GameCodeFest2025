package services;

public class PriorityTask {
    public String name;
    public int priorityPoint;
    public int distanceToPlayer;

    public PriorityTask(String name, int priorityPoint, int distanceToPlayer) {
        this.name = name;
        this.priorityPoint = priorityPoint;
        this.distanceToPlayer = distanceToPlayer;
    }
}
