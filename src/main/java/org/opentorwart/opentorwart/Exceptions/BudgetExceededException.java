package org.opentorwart.opentorwart.Exceptions;

public class BudgetExceededException extends RuntimeException {
    public BudgetExceededException(String team) {
        super("Budget exceeded for team " + team);
    }
}
