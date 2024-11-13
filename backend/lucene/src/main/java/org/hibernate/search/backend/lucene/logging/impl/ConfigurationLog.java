/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET;
import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET_LEGACY_ENGINE;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.EventContextFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

import org.apache.lucene.util.Version;

@CategorizedLogger(
		category = ConfigurationLog.CATEGORY_NAME,
		description = """
				Logs information on the Lucene compatibility version used in the index.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ConfigurationLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.configuration";

	ConfigurationLog INSTANCE = LoggerFactory.make( ConfigurationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 75,
			value = "Missing value for configuration property '%1$s': using LATEST (currently '%2$s'). %3$s")
	void recommendConfiguringLuceneVersion(String key, Version latest,
			@FormatWith(EventContextFormatter.class) EventContext context);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 228,
			value = "Unable to parse '%1$ss' into a Lucene version: %2$s")
	SearchException illegalLuceneVersionFormat(String property, String luceneErrorMessage, @Cause Exception e);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 30,
			value = "Invalid multi-tenancy strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidMultiTenancyStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 31,
			value = "Invalid tenant identifiers: '%1$s'."
					+ " No tenant identifier is expected, because multi-tenancy is disabled for this backend.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(Set<String> tenantId, @Param EventContext context);

	@Message(id = ID_OFFSET + 32,
			value = "Missing tenant identifier."
					+ " A tenant identifier is expected, because multi-tenancy is enabled for this backend.")
	SearchException multiTenancyEnabledButNoTenantIdProvided(@Param EventContext context);

	@Message(id = ID_OFFSET + 87,
			value = "Invalid filesystem access strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidFileSystemAccessStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 88,
			value = "Invalid locking strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidLockingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 89,
			value = "Incorrect sharding strategy implementation:"
					+ " strategy '%1$s' did not declare any shard identifiers during initialization."
					+ " Declare shard identifiers using context.shardIdentifiers(...) or,"
					+ " if sharding is disabled, call context.disableSharding().")
	SearchException missingShardIdentifiersAfterShardingStrategyInitialization(Object strategy);

	@Message(id = ID_OFFSET + 90,
			value = "When using sharding strategy '%1$s', this configuration property must be set.")
	SearchException missingPropertyValueForShardingStrategy(String strategyName);

	@Message(id = ID_OFFSET + 91,
			value = "Invalid routing key: '%1$s'. Valid keys are: %2$s.")
	SearchException invalidRoutingKeyForExplicitShardingStrategy(String invalidKey, Collection<String> validKeys);

	@Message(id = ID_OFFSET + 108,
			value = "Invalid I/O strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidIOStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 123,
			value = "Invalid value for IndexWriter setting '%1$s': '%2$s'. %3$s")
	SearchException illegalIndexWriterSetting(String settingName, Object settingValue, String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 124,
			value = "Invalid value for merge policy setting '%1$s': '%2$s'. %3$s")
	SearchException illegalMergePolicySetting(String settingName, Object settingValue, String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 146,
			value = "Unable to apply query caching configuration: %1$s")
	SearchException unableToApplyQueryCacheConfiguration(String errorMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 148,
			value = "Invalid backend configuration: mapping requires multi-tenancy"
					+ " but no multi-tenancy strategy is set.")
	SearchException multiTenancyRequiredButExplicitlyDisabledByBackend();

	@Message(id = ID_OFFSET + 149,
			value = "Invalid backend configuration: mapping requires single-tenancy"
					+ " but multi-tenancy strategy is set.")
	SearchException multiTenancyNotRequiredButExplicitlyEnabledByTheBackend();

}
