/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.client5.http.async.HttpAsyncClient;

class ClientRest5ElasticsearchExtensionIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );


	private SearchIntegration integration;

	@BeforeEach
	void setup() {
		this.integration = setupHelper.start().withIndexes( mainIndex ).setup().integration();
	}

	@Test
	void backend_getClient() throws Exception {
		Backend backend = integration.backend();
		ElasticsearchBackend elasticsearchBackend = backend.unwrap( ElasticsearchBackend.class );
		Rest5Client restClient = elasticsearchBackend.client( Rest5Client.class );

		// Test that the client actually works
		Response response = restClient.performRequest( new Request( "GET", "/" ) );
		assertThat( response.getStatusCode() ).isEqualTo( 200 );
	}

	@Test
	void backend_getClient_error_invalidClass() {
		Backend backend = integration.backend();
		ElasticsearchBackend elasticsearchBackend = backend.unwrap( ElasticsearchBackend.class );

		assertThatThrownBy( () -> elasticsearchBackend.client( HttpAsyncClient.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid requested type for client",
						HttpAsyncClient.class.getName(),
						"The Elasticsearch low-level client can only be unwrapped to",
						Rest5Client.class.getName()
				);
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
		}
	}
}
