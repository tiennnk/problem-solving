package io.github.tiennnk.problemsolving.protocol.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.github.tiennnk.problemsolving.task.domain.TaskCreated;

@Component
public class TaskCreatedListener {

  private static final Logger log = LoggerFactory.getLogger(TaskCreatedListener.class);

  private final ProtocolRegistry protocolRegistry;

  public TaskCreatedListener(ProtocolRegistry protocolRegistry) {
    this.protocolRegistry = protocolRegistry;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTaskCreated(TaskCreated event) {
    log.info("Starting {} protocol for task {} (event {})", event.protocolType(), event.taskId(), event.eventId());
    try {
      protocolRegistry.get(event.protocolType()).run(event.taskId());
    } catch (RuntimeException ex) {
      log.error("Protocol for task {} crashed", event.taskId(), ex);
    }
  }
}
