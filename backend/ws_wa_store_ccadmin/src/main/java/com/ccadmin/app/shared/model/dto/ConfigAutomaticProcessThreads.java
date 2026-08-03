package com.ccadmin.app.shared.model.dto;

public class ConfigAutomaticProcessThreads {

    public String keyConfig;
    public String DescriptionConfig;
    public Long InitialDelay;
    public Long ExecutionCycle;
    public int NumberThreads;
    public int NumberProcessesThread;

    public ConfigAutomaticProcessThreads(String keyConfig, String descriptionConfig, Long initialDelay, Long executionCycle, int numberThreads, int numberProcessesThread) {
        this.keyConfig = keyConfig;
        DescriptionConfig = descriptionConfig;
        InitialDelay = initialDelay;
        ExecutionCycle = executionCycle;
        NumberThreads = numberThreads;
        NumberProcessesThread = numberProcessesThread;
    }
}
