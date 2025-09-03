/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.logging.spi;

import static org.hibernate.search.backend.elasticsearch.client.common.logging.spi.ElasticsearchClientCommonLog.ID_BACKEND_OFFSET;
import static org.hibernate.search.backend.elasticsearch.client.common.logging.spi.ElasticsearchClientCommonLog.ID_OFFSET;
import static org.hibernate.search.backend.elasticsearch.client.common.logging.spi.ElasticsearchClientCommonLog.ID_OFFSET_LEGACY_ES;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = ElasticsearchClientConfigurationLog.CATEGORY_NAME,
		description = """
				Logs related to the Elasticsearch-specific backend configuration.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ElasticsearchClientConfigurationLog {
	String CATEGORY_NAME = "org.hibernate.search.configuration.elasticsearch.client";

	ElasticsearchClientConfigurationLog INSTANCE =
			LoggerFactory.make( ElasticsearchClientConfigurationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@Message(id = ID_OFFSET_LEGACY_ES + 22, value = "Invalid index status: '%1$s'."
			+ " Valid statuses are: %2$s.")
	SearchException invalidIndexStatus(String invalidRepresentation, List<String> validRepresentations);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ES + 73,
			value = "Hibernate Search will connect to Elasticsearch with authentication over plain HTTP (not HTTPS)."
					+ " The password will be sent in clear text over the network.")
	void usingPasswordOverHttp();

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_BACKEND_OFFSET + 89, value = "Invalid host/port: '%1$s'."
			+ " The host/port string must use the format 'host:port', for example 'mycompany.com:9200'"
			+ " The URI scheme ('http://', 'https://') must not be included.")
	SearchException invalidHostAndPort(String hostAndPort, @Cause Exception e);

	@Message(id = ID_BACKEND_OFFSET + 126, value = "Invalid target hosts configuration:"
			+ " both the 'uris' property and the 'protocol' property are set."
			+ " Uris: '%1$s'. Protocol: '%2$s'."
			+ " Either set the protocol and hosts simultaneously using the 'uris' property,"
			+ " or set them separately using the 'protocol' property and the 'hosts' property.")
	SearchException uriAndProtocol(List<String> uris, String protocol);

	@Message(id = ID_BACKEND_OFFSET + 127, value = "Invalid target hosts configuration:"
			+ " both the 'uris' property and the 'hosts' property are set."
			+ " Uris: '%1$s'. Hosts: '%2$s'."
			+ " Either set the protocol and hosts simultaneously using the 'uris' property,"
			+ " or set them separately using the 'protocol' property and the 'hosts' property.")
	SearchException uriAndHosts(List<String> uris, List<String> hosts);

	@Message(id = ID_BACKEND_OFFSET + 128,
			value = "Invalid target hosts configuration: the 'uris' use different protocols (http, https)."
					+ " All URIs must use the same protocol. Uris: '%1$s'.")
	SearchException differentProtocolsOnUris(List<String> uris);

	@Message(id = ID_BACKEND_OFFSET + 129,
			value = "Invalid target hosts configuration: the list of hosts must not be empty.")
	SearchException emptyListOfHosts();

	@Message(id = ID_BACKEND_OFFSET + 130,
			value = "Invalid target hosts configuration: the list of URIs must not be empty.")
	SearchException emptyListOfUris();

	// -----------------------------------
	// New messages from Search 8.2 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 1, value = "Invalid uri: '%1$s'. Reason: %2$s")
	SearchException invalidUri(String uri, String reason, @Cause Exception e);

}
