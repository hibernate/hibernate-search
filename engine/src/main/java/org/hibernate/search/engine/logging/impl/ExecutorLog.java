/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;
import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET_LEGACY;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = ExecutorLog.CATEGORY_NAME,
		description = """
				Logs the information on various executor operations.
				The executor, in this case in particular, can be the one used to batch the index work.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ExecutorLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.executor";

	ExecutorLog INSTANCE = LoggerFactory.make( ExecutorLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_LEGACY + 230, value = "Starting executor '%1$s'")
	void startingExecutor(String name);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_LEGACY + 231, value = "Stopping executor '%1$s'")
	void stoppingExecutor(String indexName);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 62,
			value = "Unable to submit work to '%1$s': thread received interrupt signal."
					+ " The work has been discarded.")
	SearchException threadInterruptedWhileSubmittingWork(String orchestratorName);

	@Message(id = ID_OFFSET + 63,
			value = "Unable to submit work to '%1$s': this orchestrator is stopped."
					+ " The work has been discarded.")
	SearchException submittedWorkToStoppedOrchestrator(String orchestratorName);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 130, value = "Processing %d works in executor '%s'")
	void numberOfWorksInExecutor(int workCount, String name);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 131, value = "Processed %d works in executor '%s'")
	void numberOfProcessedWorksInExecutor(int workCount, String name);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 132, value = "Scheduling task '%s'.")
	void schedulingTask(String name);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 133, value = "Running task '%s'")
	void runningTask(String name);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 134, value = "Completed task '%s'")
	void completedTask(String name);

}
