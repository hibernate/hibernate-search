package org.hibernate.search.backend.elasticsearch.client.common.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonHttpEntityContentProvider;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchValidationModeTest {
	@Test
	void applicationAndCustomClientIngressRemainValidatedInProductionMode() {
		var cycle = new ObjectNode( JsonNodeFactory.instance );
		cycle.set( "cycle", cycle );
		assertThatThrownBy( () -> ElasticsearchJsonRequest.post().bodyJson( cycle ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> ElasticsearchJsonResponse.withJsonBody( "custom", 200, "ok", cycle ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> JsonHandlerBridge.importObject( JacksonJsonHandlers.object(), cycle, "user input" ) )
				.hasMessageContaining( "user input" );
	}

	@Test
	void internalRequestWrappingAndTrustedExportsDoNotRescanWhenDisabled() {
		var fields = new CountingMap();
		fields.put( "value", IntNode.valueOf( 42 ) );
		var tree = new ObjectNode( JsonNodeFactory.instance, fields );
		var request = ElasticsearchJsonRequest.post().bodyJsonFromInternal( tree ).build();
		assertThat( fields.visits ).isZero();
		fields.visits = 0;
		assertThat( request.bodyParts( JacksonJsonHandlers.object() ).get( 0 ) ).isSameAs( tree );
		assertThat( fields.visits ).isEqualTo( JsonTreeValidation.isInternalValidationEnabled() ? 1 : 0 );
	}

	@Test
	void internallyParsedResponseWrappingAndExportsDoNotRescanWhenDisabled() {
		var fields = new CountingMap();
		fields.put( "value", IntNode.valueOf( 42 ) );
		var tree = new ObjectNode( JsonNodeFactory.instance, fields );
		var response = ElasticsearchJsonResponse.withInternalJsonBody( "builtin", 200, "ok", tree );
		assertThat( fields.visits ).isEqualTo( JsonTreeValidation.isInternalValidationEnabled() ? 1 : 0 );
		fields.visits = 0;
		assertThat( response.body( JacksonJsonHandlers.object() ) ).isSameAs( tree );
		assertThat( fields.visits ).isEqualTo( JsonTreeValidation.isInternalValidationEnabled() ? 1 : 0 );
	}

	@Test
	void scopedRequestAndResponseExportsRemainIndependent() {
		var fields = new CountingMap();
		fields.put( "value", IntNode.valueOf( 42 ) );
		var tree = new ObjectNode( JsonNodeFactory.instance, fields );
		var original = ElasticsearchJsonRequest.post().bodyJsonFromInternal( tree ).build();
		for ( boolean enabled : new boolean[] { false, true, false } ) {
			var validation = JsonValidation.of( enabled );
			fields.visits = 0;
			var request = original.withValidation( validation );
			assertThat( request.bodyParts( JacksonJsonHandlers.object() ).get( 0 ) ).isSameAs( tree );
			assertThat( fields.visits ).isEqualTo( enabled ? 1 : 0 );
			fields.visits = 0;
			var response = ElasticsearchJsonResponse.withInternalJsonBody( "builtin", 200, "ok", tree, validation );
			assertThat( fields.visits ).isEqualTo( enabled ? 1 : 0 );
			fields.visits = 0;
			assertThat( response.body( JacksonJsonHandlers.object() ) ).isSameAs( tree );
			assertThat( fields.visits ).isEqualTo( enabled ? 1 : 0 );
		}
	}

	@Test
	void transportBodyUsesCodecContextForInternalChecks() throws Exception {
		var fields = new CountingMap();
		fields.put( "value", IntNode.valueOf( 42 ) );
		var tree = new ObjectNode( JsonNodeFactory.instance, fields );
		var disabled = JsonProviderHelper.create( false, JsonValidation.of( false ) ).getCodec();
		var enabled = JsonProviderHelper.create( false, JsonValidation.of( true ) ).getCodec();
		new JsonHttpEntityContentProvider(
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
						.ndjson( List.of( tree ), disabled.validation() ) )
				.close();
		int baselineVisits = fields.visits;
		fields.visits = 0;
		new JsonHttpEntityContentProvider(
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
						.ndjson( List.of( tree ), enabled.validation() ) )
				.close();
		assertThat( fields.visits ).isEqualTo( baselineVisits + 1 );
		fields.visits = 0;
		new JsonHttpEntityContentProvider(
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
						.ndjson( List.of( tree ), disabled.validation() ) )
				.close();
		assertThat( fields.visits ).isEqualTo( baselineVisits );
	}

	@Test
	void prettyOutputChecksItsWholeTreeWhenEnabled() {
		var cycle = new ObjectNode( JsonNodeFactory.instance );
		cycle.set( "cycle", cycle );
		var codec = JsonProviderHelper.createUserFacingCodec( JsonValidation.of( true ) );
		assertThatThrownBy( () -> codec.write( cycle ) ).isInstanceOf( IllegalArgumentException.class );
	}

	private static final class CountingMap extends LinkedHashMap<String, JsonNode> {
		private int visits;

		@Override
		public Set<Map.Entry<String, JsonNode>> entrySet() {
			visits++;
			return super.entrySet();
		}
	}
}
