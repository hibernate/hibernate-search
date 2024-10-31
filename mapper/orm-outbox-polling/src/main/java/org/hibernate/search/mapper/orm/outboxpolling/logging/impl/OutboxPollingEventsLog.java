/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.mapper.orm.outboxpolling.logging.impl;

import static org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.PersistenceException;

import org.hibernate.search.engine.logging.impl.EngineLog;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ToStringTreeMultilineFormatter;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = OutboxPollingEventsLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface OutboxPollingEventsLog extends BasicLogger {

	String CATEGORY_NAME = "org.hibernate.search.mapper.outboxpolling";

	OutboxPollingEventsLog INSTANCE = LoggerFactory.make( OutboxPollingEventsLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 3, value = "Max '%1$s' retries exhausted to process the event. Event will be aborted.")
	SearchException maxRetryExhausted(int retries);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 4, value = "Background indexing failed for event #%1$s on entity of type '%2$s' with ID '%3$s'."
			+ " Attempts so far: %4$d. The event will be reprocessed after the moment: %5$s.")
	void backgroundIndexingRetry(UUID eventId, String entityName, String entityId, int attempts, Instant processAfter);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 5, value = "Starting outbox event processor '%1$s': %2$s")
	void startingOutboxEventProcessor(String name,
			@FormatWith(ToStringTreeMultilineFormatter.class) ToStringTreeAppendable processor);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 6, value = "Stopping outbox event processor '%1$s'")
	void stoppingOutboxEventProcessor(String name);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 11, value = "'%1$s' failed to obtain a lock on events to update/delete; will try again later.")
	void outboxEventProcessorUnableToLock(String name, @Cause PersistenceException lockException);

	@Message(id = ID_OFFSET + 12, value = "Unable to serialize OutboxEvent payload with Avro: %1$s")
	SearchException unableToSerializeOutboxEventPayloadWithAvro(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 13, value = "Unable to deserialize OutboxEvent payload with Avro: %1$s")
	SearchException unableToDeserializeOutboxEventPayloadWithAvro(String causeMessage, @Cause Throwable cause);

	@Message(value = "Pulse operation for agent '%1$s'")
	String outboxEventProcessorPulse(AgentReference agentReference);

	@Message(id = ID_OFFSET + 17, value = "Agent '%1$s': failed to infer a target cluster from the list of registered agents."
			+ " The agent will try again in the next pulse."
			+ " Cause: %2$s"
			+ " Registered agents: %3$s.")
	SearchException outboxEventProcessorPulseFailed(AgentReference agentReference, String causeMessage,
			List<Agent> allAgentsInIdOrder,
			@Cause RuntimeException cause);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 18, value = "Agent '%1$s': the registration of some agents in the outbox-polling strategy"
			+ " are considered expired and will be forcibly removed: %2$s."
			+ " These agents did not update their registration in the database in time."
			+ " This can be caused by invalid configuration (expiration lower than how long it takes to process a batch of events)"
			+ " or by an application node being forcibly stopped (disconnection from the network, application crash).")
	void removingTimedOutAgents(AgentReference agentReference, List<Agent> timedOutAgents);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 21, value = "Starting outbox mass indexer agent '%1$s': %2$s")
	void startingOutboxMassIndexerAgent(String name,
			@FormatWith(ToStringTreeMultilineFormatter.class) ToStringTreeAppendable processor);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 22, value = "Stopping outbox mass indexer agent '%1$s'")
	void stoppingOutboxMassIndexerAgent(String name);

	@Message(id = EngineLog.ID_OFFSET + 28, value = "Agent '%1$s': could not find the agent after starting a new transaction."
			+ " The agent was present just a moment ago."
			+ " Either this problem is a rare occurrence, or the pulse expiration delay is too short.")
	SearchException agentRegistrationIneffective(AgentReference agentReference);

	@Message(id = EngineLog.ID_OFFSET + 29, value = "Nonblocking operation submitter is not supported.")
	SearchException nonblockingOperationSubmitterNotSupported();

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 34,
			// Warning: we check that this message does NOT appear in logs in some tests.
			// If you update this message, make sure to also update OutboxPollingAutomaticIndexingConcurrencyIT.
			value = "'%1$s' failed to retrieve events to process due to a locking failure; will try again later.")
	void eventProcessorFindEventsUnableToLock(String name, @Cause PersistenceException lockException);
}
