/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;
import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET_LEGACY_ES;
import static org.jboss.logging.Logger.Level.DEBUG;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = ConfigurationLog.CATEGORY_NAME,
		description = """
				Logs related to the Elasticsearch-specific backend configuration.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ConfigurationLog {
	String CATEGORY_NAME = "org.hibernate.search.configuration.elasticsearch";

	ConfigurationLog INSTANCE = LoggerFactory.make( ConfigurationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@Message(id = ID_OFFSET_LEGACY_ES + 22, value = "Invalid index status: '%1$s'."
			+ " Valid statuses are: %2$s.")
	SearchException invalidIndexStatus(String invalidRepresentation, List<String> validRepresentations);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 15, value = "Invalid multi-tenancy strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidMultiTenancyStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 16,
			value = "Invalid tenant identifiers: '%1$s'."
					+ " No tenant identifier is expected, because multi-tenancy is disabled for this backend.")
	SearchException tenantIdProvidedButMultiTenancyDisabled(Set<String> tenantIds, @Param EventContext context);

	@Message(id = ID_OFFSET + 17,
			value = "Missing tenant identifier."
					+ " A tenant identifier is expected, because multi-tenancy is enabled for this backend.")
	SearchException multiTenancyEnabledButNoTenantIdProvided(@Param EventContext context);

	@Message(id = ID_OFFSET + 58, value = "Invalid Elasticsearch distribution name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidElasticsearchDistributionName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 91, value = "Invalid name for the type-name mapping strategy: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidTypeNameMappingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 121, value = "Invalid dynamic type: '%1$s'."
			+ " Valid values are: %2$s.")
	SearchException invalidDynamicType(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 148,
			value = "Invalid backend configuration: mapping requires multi-tenancy"
					+ " but no multi-tenancy strategy is set.")
	SearchException multiTenancyRequiredButExplicitlyDisabledByBackend();

	@Message(id = ID_OFFSET + 149,
			value = "Invalid backend configuration: mapping requires single-tenancy"
					+ " but multi-tenancy strategy is set.")
	SearchException multiTenancyNotRequiredButExplicitlyEnabledByTheBackend();

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 192,
			value = "Elasticsearch backend will use client factory '%s'. Context: %s")
	void backendClientFactory(BeanHolder<?> clientFactoryHolder, String eventContext);

	@Message(id = ID_OFFSET + 194,
			value = "Elasticsearch backend have found no client factories. Please make one of the client factory implementations available. Context: %s")
	SearchException backendClientFactoryNotConfigured(String eventContext);

	@Message(id = ID_OFFSET + 195,
			value = "Elasticsearch backend have found multiple client factories: %s. Please make just one of the client factory implementations available. Context: %s")
	SearchException backendClientFactoryMultipleConfigured(List<BeanHolder<ElasticsearchClientFactory>> factories,
			String eventContext);

}
