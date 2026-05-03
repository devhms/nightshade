package com.example.samplerepo;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample repository file — used to verify Nightshade obfuscation.
 *
 * Contains common Java patterns: loops, conditionals, string handling,
 * field declarations. Run Nightshade on this to see all 5 strategies.
 */
public class Calculator {

    private double result;
    private int operationCount;
    private String lastOperation;
    private List<String> history;

    public Calculator() {
        this.result = 0.0;
        this.operationCount = 0;
        this.lastOperation = "none";
        this.history = new ArrayList<>();
    }

    public double add(double value) {
        // addition operation with result tracking
        result += value;
        operationCount++;
        lastOperation = "add";
        history.add("add(" + value + ") = " + result);
        return result;
    }

    public double subtract(double value) {
        // subtraction operation — updates result
        result -= value;
        operationCount++;
        lastOperation = "subtract";
        history.add("sub(" + value + ") = " + result);
        return result;
    }

    public double multiply(double value) {
        // multiplication — guards against zero
        if (value == 0.0) {
            result = 0.0;
        } else {
            result *= value;
        }
        operationCount++;
        lastOperation = "multiply";
        history.add("mul(" + value + ") = " + result);
        return result;
    }

    public double divide(double divisor) {
        // safe division — throws on zero
        if (divisor == 0.0) {
            throw new ArithmeticException("Division by zero");
        }
        result /= divisor;
        operationCount++;
        lastOperation = "divide";
        history.add("div(" + divisor + ") = " + result);
        return result;
    }

    public double power(double exponent) {
        // power using repeated multiplication loop
        double base = result;
        result = 1.0;
        int intExp = (int) Math.abs(exponent);
        for (int i = 0; i < intExp; i++) {
            result *= base;
        }
        if (exponent < 0) {
            result = 1.0 / result;
        }
        operationCount++;
        lastOperation = "power";
        history.add("pow(" + exponent + ") = " + result);
        return result;
    }

    public void reset() {
        // reset all state to initial values
        result = 0.0;
        operationCount = 0;
        lastOperation = "none";
        history.clear();
    }

    public String getHistory() {
        // build history string with line breaks
        StringBuilder sb = new StringBuilder();
        for (String entry : history) {
            sb.append(entry).append("\n");
        }
        return sb.toString();
    }

    public double getResult()       { return result; }
    public int getOperationCount()  { return operationCount; }
    public String getLastOperation(){ return lastOperation; }
}
