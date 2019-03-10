package ca.polymtl.inf8480.tp2.shared;

import java.io.Serializable;
import java.util.ArrayList;

public class Task implements Serializable {
    private ArrayList<TaskElement> operations = new ArrayList<>();

    public Task() {
    }

    public Task(ArrayList<TaskElement> operations) {
        this.operations = operations;
    }

    public void addOperation(TaskElement o) {
        this.operations.add(o);
    }

    public ArrayList<TaskElement> getOperations() {
        return this.operations;
    }

    public int getSize() {
        return this.operations.size();
    }
}
