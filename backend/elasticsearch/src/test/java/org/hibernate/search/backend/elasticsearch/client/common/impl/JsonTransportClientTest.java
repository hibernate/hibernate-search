package org.hibernate.search.backend.elasticsearch.client.common.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class JsonTransportClientTest {
	@Test
	@SuppressWarnings("unused")
	void absentTransportResultStillSupportsNativeAndHandlerAccess() {
		var transport = mock( ElasticsearchClientImplementor.class );
		when( transport.submit( any(), any() ) )
				.thenAnswer( invocation -> java.util.concurrent.CompletableFuture.completedFuture(
						new org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse<>( "host", 204,
								"empty", null ) ) );
		var client = new JsonTransportClient( transport, JsonProviderHelper.create( false ) );
		var response = client.submit( ElasticsearchJsonRequest.get().build() ).join();
		assertThat( response.statusCode() ).isEqualTo( 204 );
		assertThat( response.bodyJson() ).isNull();
		assertThat( response.body( org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers.object() ) )
				.isNull();
		assertThatThrownBy( () -> response.body( null ) )
				.isInstanceOf( org.hibernate.search.util.common.SearchException.class );
	}

	@Test
	void serializationProbeFailureCompletesFutureWithoutSubmittingTransport() {
		var failure = new IllegalStateException( "serialization probe failed" );
		Map<String, JsonNode> fields = new LinkedHashMap<>() {
			@Override
			public Set<Map.Entry<String, JsonNode>> entrySet() {
				throw failure;
			}
		};
		fields.put( "value", JsonNodeFactory.instance.numberNode( 1 ) );
		var tree = new ObjectNode( JsonNodeFactory.instance, fields );
		var transport = mock( ElasticsearchClientImplementor.class );
		var client = new JsonTransportClient( transport, JsonProviderHelper.create( false, JsonValidation.of( false ) ) );
		var request = ElasticsearchJsonRequest.post().bodyJsonFromInternal( tree ).build();

		var result = client.submit( request );

		assertThat( result ).isCompletedExceptionally();
		assertThatThrownBy( result::join ).hasCause( failure );
		verifyNoInteractions( transport );
	}
}
