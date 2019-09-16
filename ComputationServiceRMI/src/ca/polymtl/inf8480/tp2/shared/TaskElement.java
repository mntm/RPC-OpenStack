package ca.polymtl.inf8480.tp2.shared;

import java.io.Serializable;

public class TaskElement implements Serializable {
    private OperationType type;
    private int operand;

    public TaskElement(OperationType type, int operand) {
        this.type = type;
        this.operand = operand;
    }

    public int result() {
        return (this.type == OperationType.PELL) ?
                Operations.pell(this.operand) :
                Operations.prime(this.operand);
    }
}
