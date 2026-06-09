package com.doruk.argue;

import java.util.*;

public class ArgParser {
    private enum ValueType {
        SINGLE,
        MULTI,
        NONE
    }

    private List<String> optionList;
    private Map<String, ValueType> expectedArgs;
    private List<String> defaultArgs;

    private Set<String> flag;
    private Map<String, String> values;
    private Map<String, List<String>> multiValues;

    public ArgParser() {
        this.optionList = new ArrayList<>();
        this.expectedArgs = new HashMap<>();
        this.defaultArgs = new ArrayList<>();
        this.values = new HashMap<>();
        this.multiValues = new HashMap<>();
        this.flag = new HashSet<>();
    }

    private void validateShortArg(String arg) {
        if (arg.length() != 1)
            throw new IllegalArgumentException("Short arg must be a single character");
    }

    private Map<String, ValueType> extractArg(String arg) {
        var val = arg.startsWith("--") ? arg.substring(2) :
                arg.startsWith("-") ? arg.substring(1) : null;

        if (val == null)
            return null;

        if (!expectedArgs.containsKey(val))
            throw new IllegalArgumentException("Unknown argument: " + val);

        return Map.of(val, expectedArgs.get(val));
    }

    public void arg(String shortArg, String longArg, String description) {
        validateShortArg(shortArg);
        this.optionList.add(String.format("-%s, --%s\t\t%s", shortArg, longArg, description));
        this.expectedArgs.put(shortArg, ValueType.NONE);
        this.expectedArgs.put(longArg, ValueType.NONE);
    }

    public void valueArg(String shortArg, String longArg, String description) {
        validateShortArg(shortArg);
        this.optionList.add(String.format("-%s, --%s\t\t%s", shortArg, longArg, description));
        this.expectedArgs.put(shortArg, ValueType.SINGLE);
        this.expectedArgs.put(longArg, ValueType.SINGLE);
    }

    public void multiValueArg(String shortArg, String longArg, String description) {
        validateShortArg(shortArg);
        this.optionList.add(String.format("-%s, --%s\t\t%s", shortArg, longArg, description));
        this.expectedArgs.put(shortArg, ValueType.MULTI);
        this.expectedArgs.put(longArg, ValueType.MULTI);
    }

    public void shortArg(String arg, String description) {
        validateShortArg(arg);
        this.optionList.add(String.format("-%s\t\t%s", arg, description));
        this.expectedArgs.put(arg, ValueType.NONE);
    }

    public void shortValueArg(String arg, String description) {
        validateShortArg(arg);
        this.optionList.add(String.format("-%s\t\t%s", arg, description));
        this.expectedArgs.put(arg, ValueType.SINGLE);
    }

    public void shortMultiValueArg(String arg, String description) {
        validateShortArg(arg);
        this.optionList.add(String.format("-%s\t\t%s", arg, description));
        this.expectedArgs.put(arg, ValueType.MULTI);
    }

    public void longArg(String arg, String description) {
        this.optionList.add(String.format("--%s\t\t%s", arg, description));
        this.expectedArgs.put(arg, ValueType.NONE);
    }

    public void longValueArg(String arg, String description) {
        this.optionList.add(String.format("--%s\t\t%s", arg, description));
        this.expectedArgs.put(arg, ValueType.SINGLE);
    }

    public void longMultiValueArg(String arg, String description) {
        this.optionList.add(String.format("--%s\t\t%s", arg, description));
        this.expectedArgs.put(arg, ValueType.MULTI);
    }

    public boolean containsFlag(String argName) {
        return this.flag.contains(argName);
    }

    public String getValue(String argName) {
        return this.values.getOrDefault(argName, null);
    }

    public List<String> getValues(String argName) {
        return this.multiValues.get(argName);
    }

    public List<String> getDefaultArgs() {
        return this.defaultArgs;
    }

    public void parse(String[] args) {
        var consumeValue = false;
        var valueConsumed = 0;

        ValueType currentType = ValueType.NONE;
        String currentArg = null;

        for (int i = 0; i < args.length; ++i) {
            var argVal = args[i];
            var optMap = extractArg(argVal);
            if (optMap == null && !consumeValue) {
                this.defaultArgs.add(argVal);
                continue;
            }
            if (optMap != null) {
                if (consumeValue && valueConsumed < 1)
                    throw new IllegalArgumentException("Value expected for previous argument, got: " + argVal);
                else {
                    valueConsumed = 0;
                    consumeValue = false;
                }
            }

            if (consumeValue) {
                valueConsumed++;
                if (currentType == ValueType.SINGLE) {
                    this.values.put(currentArg, argVal);
                    consumeValue = false;
                    valueConsumed = 0;
                }
                else
                    this.multiValues.computeIfAbsent(currentArg, k -> new ArrayList<>())
                            .add(argVal);
                continue;
            }

            var optI = optMap.entrySet().iterator().next();
            currentArg = optI.getKey();
            currentType = optI.getValue();

            if (currentType == ValueType.NONE)
                this.flag.add(currentArg);

            consumeValue = currentType == ValueType.SINGLE || ValueType.MULTI == currentType;
        }
    }

    public String getOptions() {
        return this.optionList.stream().reduce("", (a, b) -> a + "\n" + b);
    }
}