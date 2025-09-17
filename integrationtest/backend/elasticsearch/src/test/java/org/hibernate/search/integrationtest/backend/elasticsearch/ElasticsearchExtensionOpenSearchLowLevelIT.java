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

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;

class ElasticsearchExtensionOpenSearchLowLevelIT {

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
		RestClient restClient = elasticsearchBackend.client( RestClient.class );

		// Test that the client actually works
		Response response = restClient.performRequest( new Request( "GET", "/" ) );
		assertThat( response.getStatusLine().getStatusCode() ).isEqualTo( 200 );
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
						RestClient.class.getName()
				);
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
		}
	}
}
