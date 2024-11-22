/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.DEBUG;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = ConfigurationLog.CATEGORY_NAME,
		description = """
				Logs related to the outbox polling-specific configuration.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ConfigurationLog {
	String CATEGORY_NAME = "org.hibernate.search.configuration.mapper.orm.outboxpolling";

	ConfigurationLog INSTANCE = LoggerFactory.make( ConfigurationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// TODO: or should it be not under "generic" configuration but under configuration.outboxpolling ?
	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 1,
			value = "Generated entity mapping for outbox events used in the outbox-polling coordination strategy: %1$s")
	void outboxEventGeneratedEntityMapping(@FormatWith(JaxbEntityMappingsFormatter.class) JaxbEntityMappingsImpl mappings);

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

	// TODO: or should it be not under "generic" configuration but under configuration.outboxpolling ?
	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 10, value = "The outbox event processor is disabled for tenant '%s'. "
			+ " Events will accumulate in the outbox table and indexes will not be updated,"
			+ " unless another application node connects to the same database with their event processor enabled.")
	void eventProcessorDisabled(String tenantId);

	// TODO: or should it be not under "generic" configuration but under configuration.outboxpolling ?
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

	@Message(id = ID_OFFSET + 30, value = "Invalid name for the UUID generation strategy: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidUuidGenerationStrategyName(String name, List<String> values);

	@Message(id = ID_OFFSET + 31, value = "Invalid name for the outbox event processing order: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidOutboxEventProcessingOrderName(String name, List<String> values);

	@Message(id = ID_OFFSET + 32, value = "Invalid name for the payload type: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidPayloadTypeName(String name, List<String> values);

	@Message(id = ID_OFFSET + 35,
			value = "Unable to process provided entity mappings: %1$s")
	SearchException unableToProcessEntityMappings(String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 36,
			value = "Unable to parse '%1$s' as a JDBC type code or type code name. %2$s")
	SearchException unableToParseJdbcTypeCode(String value, String causeMessage, @Cause Exception cause);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 38,
			value = "Generated entity mapping for outbox events used in the outbox-polling coordination strategy: %1$s")
	void outboxEventGeneratedEntityMappingClassDetails(@FormatWith(ClassDetailsMappingsFormatter.class) ClassDetails mappings);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 39,
			value = "Generated entity mapping for agents used in the outbox-polling coordination strategy: %1$s")
	void agentGeneratedEntityMappingClassDetails(@FormatWith(ClassDetailsMappingsFormatter.class) ClassDetails mappings);
}
