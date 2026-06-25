package com.ccadmin.app.shared.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class GenericQueuedService {

    @Autowired
    private BlockingQueue<IGenericTaskService> queueTask;

    public void addQueued(IGenericTaskService taskCcadminService){
        this.queueTask.offer(taskCcadminService);
    }
}
