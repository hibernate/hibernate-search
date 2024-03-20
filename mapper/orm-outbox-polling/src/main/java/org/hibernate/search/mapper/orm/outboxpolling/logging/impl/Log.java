/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.outboxpolling.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.PersistenceException;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ToStringTreeMultilineFormatter;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.MAPPER_ORM_OUTBOX_POLLING_ID_RANGE_MIN,
				max = MessageConstants.MAPPER_ORM_OUTBOX_POLLING_ID_RANGE_MAX)
})
public interface Log extends BasicLogger {

	int ID_OFFSET = MessageConstants.MAPPER_ORM_OUTBOX_POLLING_ID_RANGE_MIN;

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 1,
			value = "Generated entity mapping for outbox events used in the outbox-polling coordination strategy: %1$s")
	void outboxEventGeneratedEntityMapping(@FormatWith(JaxbEntityMappingsFormatter.class) JaxbEntityMappingsImpl mappings);

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

	@Message(id = ID_OFFSET + 7,
			value = "The total shard count must be strictly positive.")
	SearchException invalidTotalShardCount();

	@Message(id = ID_OFFSET + 8,
			value = "Shard indices must be between 0 (inclusive) and %1d (exclusive, set by '%2$s').")
	SearchException invalidShardIndex(int totalShardCount, String totalShardCountPropertyKey);

	@Message(id = ID_OFFSET + 9,
			value = "This property must be set when '%s' is set."
	)
	SearchException missingPropertyForStaticSharding(String otherPropertyKey);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 10, value = "The outbox event processor is disabled for tenant '%s'. "
			+ " Events will accumulate in the outbox table and indexes will not be updated,"
			+ " unless another application node connects to the same database with their event processor enabled.")
	void eventProcessorDisabled(String tenantId);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 11, value = "'%1$s' failed to obtain a lock on events to update/delete; will try again later.")
	void outboxEventProcessorUnableToLock(String name, @Cause PersistenceException lockException);

	@Message(id = ID_OFFSET + 12, value = "Unable to serialize OutboxEvent payload with Avro: %1$s")
	SearchException unableToSerializeOutboxEventPayloadWithAvro(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 13, value = "Unable to deserialize OutboxEvent payload with Avro: %1$s")
	SearchException unableToDeserializeOutboxEventPayloadWithAvro(String causeMessage, @Cause Throwable cause);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 14,
			value = "Generated entity mapping for agents used in the outbox-polling coordination strategy: %1$s")
	void agentGeneratedEntityMapping(
			@FormatWith(JaxbEntityMappingsFormatter.class) JaxbEntityMappingsImpl xmlMappingDefinition);

	@Message(id = ID_OFFSET + 15, value = "The pulse interval must be greater than or equal to the polling interval"
			+ " i.e. in this case at least %s")
	SearchException invalidPollingIntervalAndPulseInterval(long pollingInterval);

	@Message(id = ID_OFFSET + 16, value = "The pulse expiration must be greater than or equal to 3 times the pulse interval"
			+ " i.e. in this case at least %s")
	SearchException invalidPulseIntervalAndPulseExpiration(long pulseInterfaceTimes3);

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

	@Message(id = ID_OFFSET + 19, value = "Agent '%1$s' is statically assigned to %2$s,"
			+ " but this conflicts with agent '%3$s' which expects %4$s shards."
			+ " This can be a temporary situation caused by some application instances being forcibly stopped and replacements being spun up,"
			+ " in which case the problem will resolve itself after a few seconds once the registration of the old instances expires."
			+ " However, if the situation persists, this indicates misconfiguration, with multiple application instances participating"
			+ " in event processing and expecting a different amount of shards;"
			+ " consider adjusting the configuration or switching to dynamic sharding.")
	SearchException conflictingOutboxEventBackgroundProcessorAgentTotalShardCountForStaticSharding(
			AgentReference reference, ShardAssignmentDescriptor staticShardAssignment,
			AgentReference conflictingAgentReference, int conflictingAgentTotalShardCount);

	@Message(id = ID_OFFSET + 20, value = "Agent '%1$s' is statically assigned to %2$s,"
			+ " but this conflicts with agent '%3$s' which is also assigned to that shard."
			+ " This can be a temporary situation caused by some application instances being forcibly stopped and replacements being spun up,"
			+ " in which case the problem will resolve itself after a few seconds once the registration of the old instances expires."
			+ " However, if the situation persists, this indicates misconfiguration, with multiple application instances participating"
			+ " in event processing and being assigned to the same shard;"
			+ " consider adjusting the configuration or switching to dynamic sharding.")
	SearchException conflictingOutboxEventBackgroundProcessorAgentShardsForStaticSharding(
			AgentReference reference, ShardAssignmentDescriptor staticShardAssignment,
			AgentReference conflictingAgentReference);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 21, value = "Starting outbox mass indexer agent '%1$s': %2$s")
	void startingOutboxMassIndexerAgent(String name,
			@FormatWith(ToStringTreeMultilineFormatter.class) ToStringTreeAppendable processor);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 22, value = "Stopping outbox mass indexer agent '%1$s'")
	void stoppingOutboxMassIndexerAgent(String name);

	@Message(id = ID_OFFSET + 23,
			value = "Invalid target for Outbox Polling extension: '%1$s'."
					+ " This extension can only be applied when Hibernate Search is configured to use the 'outbox-polling' coordination strategy.")
	SearchException outboxPollingExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET + 24,
			value = "Multi-tenancy is enabled but no tenant id is specified. Available tenants are: '%1$s'.")
	SearchException noTenantIdSpecified(Set<String> tenantIds);

	@Message(id = ID_OFFSET + 25,
			value = "Multi-tenancy is not enabled but a tenant id is specified. Trying to use the tenant id: '%1$s'.")
	SearchException multiTenancyNotEnabled(String tenantId);

	@Message(id = ID_OFFSET + 26, value = "Outbox polling agent configuration property conflict."
			+ " Either mapping property %1$s or subset of name adjustment properties %2$s should be provided at the same time.")
	SearchException agentConfigurationPropertyConflict(String mappingPropertyName, String[] nameAdjustmentProperties);

	@Message(id = ID_OFFSET + 27, value = "Outbox event configuration property conflict."
			+ " Either mapping property %1$s or subset of name adjustment properties %2$s should be provided at the same time.")
	SearchException outboxEventConfigurationPropertyConflict(String mappingPropertyName, String[] nameAdjustmentProperties);

	@Message(id = ID_OFFSET + 28, value = "Agent '%1$s': could not find the agent after starting a new transaction."
			+ " The agent was present just a moment ago."
			+ " Either this problem is a rare occurrence, or the pulse expiration delay is too short.")
	SearchException agentRegistrationIneffective(AgentReference agentReference);

	@Message(id = ID_OFFSET + 29, value = "Nonblocking operation submitter is not supported.")
	SearchException nonblockingOperationSubmitterNotSupported();

	@Message(id = ID_OFFSET + 30, value = "Invalid name for the UUID generation strategy: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidUuidGenerationStrategyName(String name, List<String> values);

	@Message(id = ID_OFFSET + 31, value = "Invalid name for the outbox event processing order: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidOutboxEventProcessingOrderName(String name, List<String> values);

	@Message(id = ID_OFFSET + 32, value = "Invalid name for the payload type: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidPayloadTypeName(String name, List<String> values);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 34,
			// Warning: we check that this message does NOT appear in logs in some tests.
			// If you update this message, make sure to also update OutboxPollingAutomaticIndexingConcurrencyIT.
			value = "'%1$s' failed to retrieve events to process due to a locking failure; will try again later.")
	void eventProcessorFindEventsUnableToLock(String name, @Cause PersistenceException lockException);

	@Message(id = ID_OFFSET + 35,
			value = "Unable to process provided entity mappings: %1$s")
	SearchException unableToProcessEntityMappings(String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 36,
			value = "Unable to parse '%1$s' as a JDBC type code or type code name. %2$s")
	SearchException unableToParseJdbcTypeCode(String value, String causeMessage, @Cause Exception cause);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 37,
			value = "Configuration property '%1$s' is configured with a deprecated value '%2$s'. "
					+ "Use '%3$s' instead.")
	void usingDeprecatedPropertyValue(String property, String value, String correctValue);
}
