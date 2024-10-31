/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;
import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET_LEGACY_ES;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = ConfigurationLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ConfigurationLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.configuration";

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

	@Message(id = ID_OFFSET + 89, value = "Invalid host/port: '%1$s'."
			+ " The host/port string must use the format 'host:port', for example 'mycompany.com:9200'"
			+ " The URI scheme ('http://', 'https://') must not be included.")
	SearchException invalidHostAndPort(String hostAndPort, @Cause Exception e);

	@Message(id = ID_OFFSET + 91, value = "Invalid name for the type-name mapping strategy: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidTypeNameMappingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 121, value = "Invalid dynamic type: '%1$s'."
			+ " Valid values are: %2$s.")
	SearchException invalidDynamicType(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET + 126, value = "Invalid target hosts configuration:"
			+ " both the 'uris' property and the 'protocol' property are set."
			+ " Uris: '%1$s'. Protocol: '%2$s'."
			+ " Either set the protocol and hosts simultaneously using the 'uris' property,"
			+ " or set them separately using the 'protocol' property and the 'hosts' property.")
	SearchException uriAndProtocol(List<String> uris, String protocol);

	@Message(id = ID_OFFSET + 127, value = "Invalid target hosts configuration:"
			+ " both the 'uris' property and the 'hosts' property are set."
			+ " Uris: '%1$s'. Hosts: '%2$s'."
			+ " Either set the protocol and hosts simultaneously using the 'uris' property,"
			+ " or set them separately using the 'protocol' property and the 'hosts' property.")
	SearchException uriAndHosts(List<String> uris, List<String> hosts);

	@Message(id = ID_OFFSET + 128,
			value = "Invalid target hosts configuration: the 'uris' use different protocols (http, https)."
					+ " All URIs must use the same protocol. Uris: '%1$s'.")
	SearchException differentProtocolsOnUris(List<String> uris);

	@Message(id = ID_OFFSET + 129,
			value = "Invalid target hosts configuration: the list of hosts must not be empty.")
	SearchException emptyListOfHosts();

	@Message(id = ID_OFFSET + 130,
			value = "Invalid target hosts configuration: the list of URIs must not be empty.")
	SearchException emptyListOfUris();

	@Message(id = ID_OFFSET + 148,
			value = "Invalid backend configuration: mapping requires multi-tenancy"
					+ " but no multi-tenancy strategy is set.")
	SearchException multiTenancyRequiredButExplicitlyDisabledByBackend();

	@Message(id = ID_OFFSET + 149,
			value = "Invalid backend configuration: mapping requires single-tenancy"
					+ " but multi-tenancy strategy is set.")
	SearchException multiTenancyNotRequiredButExplicitlyEnabledByTheBackend();
}
