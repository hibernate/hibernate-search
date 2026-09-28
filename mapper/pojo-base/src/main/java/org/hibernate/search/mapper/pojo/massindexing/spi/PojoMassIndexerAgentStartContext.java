package org.hibernate.search.mapper.pojo.massindexing.spi;

import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.reporting.FailureHandler;

public interface PojoMassIndexerAgentStartContext {

	ScheduledExecutorService scheduledExecutor();

	FailureHandler failureHandler();
}
