/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.mapper.orm.outboxpolling.logging.impl;

import static org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.PersistenceException;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ClusterDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ToStringTreeMultilineFormatter;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = OutboxPollingEventsLog.CATEGORY_NAME,
		description = """
				The main category for the outbox polling-specific logs.
				It may also include logs that do not fit any other, more specific, outbox polling category.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface OutboxPollingEventsLog {

	String CATEGORY_NAME = "org.hibernate.search.mapper.orm.outboxpolling";

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

	@Message(id = ID_OFFSET + 28, value = "Agent '%1$s': could not find the agent after starting a new transaction."
			+ " The agent was present just a moment ago."
			+ " Either this problem is a rare occurrence, or the pulse expiration delay is too short.")
	SearchException agentRegistrationIneffective(AgentReference agentReference);

	@Message(id = ID_OFFSET + 29, value = "Nonblocking operation submitter is not supported.")
	SearchException nonblockingOperationSubmitterNotSupported();

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 34,
			// Warning: we check that this message does NOT appear in logs in some tests.
			// If you update this message, make sure to also update OutboxPollingAutomaticIndexingConcurrencyIT.
			value = "'%1$s' failed to retrieve events to process due to a locking failure; will try again later.")
	void eventProcessorFindEventsUnableToLock(String name, @Cause PersistenceException lockException);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 38, value = "Agent '%s': registering.")
	void agentRegistering(AgentReference agentReference);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 39, value = "Agent '%s': leaving cluster.")
	void agentLeaving(AgentReference agentReference);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 40, value = "Agent '%s': suspending.")
	void agentSuspending(AgentReference agentReference);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 41, value = "Agent '%s': waiting for cluster changes. Shard assignment: %s. Cluster: %s")
	void agentWaiting(AgentReference agentReference, ShardAssignmentDescriptor shardAssignment,
			ClusterDescriptor clusterDescriptor);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 42, value = "Agent '%s': running. Shard assignment: %s. Cluster: %s")
	void agentRunning(AgentReference agentReference, ShardAssignmentDescriptor shardAssignment,
			ClusterDescriptor clusterDescriptor);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 43, value = "Agent '%s': starting pulse at %s with self = %s, all agents = %s")
	void agentPulseStarting(AgentReference agentReference, Instant now, Agent self, List<Agent> allAgentsInIdOrder);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 44, value = "Agent '%s': ending pulse at %s with self = %s")
	void agentPulseEnded(AgentReference agentReference, Instant now, Agent self);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 45, value = "Agent '%s': reassessing the new situation in the next pulse")
	void agentReassessing(AgentReference agentReference);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 46, value = "Processing %d outbox events for '%s': '%s'")
	void processingOutboxEvents(int size, String name, List<OutboxEvent> events);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 47, value = "Agent '%s': waiting for event processor '%s', which has not reached state '%s' yet")
	void agentWaitingForEvents(AgentReference agentReference, AgentReference reference, AgentState expectedState);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 48, value = "Agent '%s': all event processors reached the expected state %s")
	void agentProcessorsExpired(AgentReference agentReference, AgentState expectedState);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 49,
			value = "Agent '%s': instructions are to hold off mass indexing and to retry a pulse in %s, around %s")
	void agentHoldMassIndexing(AgentReference agentReference, Duration delay, Instant expiration);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 50,
			value = "Agent '%s': instructions are to proceed with mass indexing and to retry a pulse in %s, around %s")
	void agentProceedMassIndexing(AgentReference agentReference, Duration pulseInterval, Instant expiration);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 51, value = "Agent '%s': the persisted shard assignment (%s) does not match the target."
			+ " Target assignment: %s."
			+ " Cluster: %s.")
	void agentAssignmentDoesNotMatchTarget(AgentReference agentReference, ShardAssignmentDescriptor persistedShardAssignment,
			ShardAssignmentDescriptor targetShardAssignment, ClusterDescriptor descriptor);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 52, value = "Agent '%s': assigning to %s")
	void agentAssignment(AgentReference agentReference, ShardAssignmentDescriptor targetShardAssignment);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 53, value = "Agent '%s': waiting for agent '%s', which has not reached state '%s' yet")
	void agentWaitingAgentReachState(AgentReference agentReference, AgentReference reference, AgentState expectedState);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 54, value = "Agent '%s': agents excluded from the cluster reached the expected state %s")
	void agentExcluded(AgentReference agentReference, AgentState expectedState);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 55,
			value = "Agent '%s': instructions are to not process events and to retry a pulse in %s, around %s")
	void instructCommitAndRetryPulseAfterDelay(AgentReference agentReference, Duration delay, Instant expiration);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 56,
			value = "Agent '%s': instructions are to process events and to retry a pulse in %s, around %s")
	void instructProceedWithEventProcessing(AgentReference agentReference, Duration pulseInterval, Instant expiration);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 57, value = "Agent '%s': waiting for agent '%s', whose state %s is not in the expected %s yet")
	void clusterMembersAreInClusterWaitingForState(AgentReference agentReference, AgentReference reference, AgentState state,
			Set<AgentState> expectedStates);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 58,
			value = "Agent '%s': waiting for agent '%s', whose total shard count %s is not the expected %s yet")
	void clusterMembersAreInClusterShardCountExpectation(AgentReference agentReference, AgentReference reference,
			Integer totalShardCount, int expectedTotalShardCount);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 59,
			value = "Agent '%s': waiting for agent '%s', whose assigned shard index %s is not the expected %s yet")
	void clusterMembersAreInClusterSharIndexExpectation(AgentReference agentReference, AgentReference reference,
			Integer assignedShardIndex, int expectedAssignedShardIndex);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 60,
			value = "Agent '%s': all cluster members reached the expected states %s and shard assignment %s")
	void clusterMembersAreInClusterReachedExpectedStates(AgentReference agentReference, Set<AgentState> expectedStates,
			ClusterDescriptor clusterDescriptor);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 61,
			value = "Agent '%s': some cluster members are missing; this agent will wait until they are present."
					+ " Target cluster: %s.")
	void agentClusterMembersMissingInfo(AgentReference agentReference, ClusterDescriptor descriptor);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 62,
			value = "Agent '%s': some cluster members are missing; this agent will wait until they are present."
					+ " Target cluster: %s.")
	void agentClusterMembersMissingTrace(AgentReference agentReference, ClusterDescriptor descriptor);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 63, value = "Agent '%s': this agent is superfluous and will not perform event processing,"
			+ " because other agents are enough to handle all the shards."
			+ " Target cluster: %s.")
	void agentSuperfluousInfo(AgentReference agentReference, ClusterDescriptor descriptor);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 64, value = "Agent '%s': this agent is superfluous and will not perform event processing,"
			+ " because other agents are enough to handle all the shards."
			+ " Target cluster: %s.")
	void agentSuperfluousTrace(AgentReference agentReference, ClusterDescriptor descriptor);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET + 65, value = "Agent '%s': another agent '%s' is currently mass indexing")
	void agentOtherAgentIsIndexingInfo(AgentReference agentReference, Agent agent);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 66, value = "Agent '%s': another agent '%s' is currently mass indexing")
	void agentOtherAgentIsIndexingTrace(AgentReference agentReference, Agent agent);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 67, value = "Persisted %d outbox events: '%s'")
	void eventPlanNumberOfPersistedEvents(int size, List<OutboxEvent> events);
}
