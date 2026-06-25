package com.ccadmin.app.shared.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Slf4j
@Component
public class GenericConsumerService {

    @Autowired
    private BlockingQueue<IGenericTaskService> queueTask;

    @PostConstruct
    public void runNoBlocking(){
        new Thread(() -> {
            while (true) {
                try {
                    IGenericTaskService genericTask = queueTask.take();
                    this.executeTask(genericTask);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Error runNoBlocking en procesar cola : "+e.getMessage(),e);
                }
            }
        }).start();
    }

    private void executeTask(IGenericTaskService genericTask){
        try{
            genericTask.execute();
        }catch (Exception ex){
            log.error("Error in executeTask :"+ex.getMessage(),ex);
        }

    }
}
