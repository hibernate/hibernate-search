package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@SuppressWarnings("deprecation") // Released Gson adapter contract is intentionally covered.
class ElasticsearchSchemaExportCharacterizationTest {

	@TempDir
	Path directory;

	@Test
	void treeBodiesRemainSharedAndCollectionsAreUnmodifiable() throws IOException {
		ObjectNode body =
				( (ObjectNode) new JsonParsing().parse( "{\"mappings\":{\"properties\":{\"name\":{\"type\":\"keyword\"}}}}" ) );
		ElasticsearchIndexSchemaExportImpl export = export( ElasticsearchJsonRequest.put().wholeEncodedPath( "/index" )
				.param( "wait_for_active_shards", "1" ).bodyJson( body ).build() );
		assertThat( export.bodyParts( JacksonJsonHandlers.object() ).get( 0 ) ).isSameAs( body );
		assertThatThrownBy(
				() -> export.bodyParts( JacksonJsonHandlers.object() ).add( new ObjectNode( JsonNodeFactory.instance ) ) )
				.isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> export.parameters().put( "key", "value" ) )
				.isInstanceOf( UnsupportedOperationException.class );
		export.toFiles( directory );
		assertThat( Files.readString( directory.resolve( "create-index.json" ) ) )
				.isEqualTo( "{\"mappings\":{\"properties\":{\"name\":{\"type\":\"keyword\"}}}}" );
		assertThat( new JsonParsing().parse( Files.readString( directory.resolve( "create-index.json" ) ) ) ).isEqualTo( body );
		assertThat( new JsonParsing().parse( Files.readString( directory.resolve( "create-index-query-params.json" ) ) ) )
				.isEqualTo( new JsonParsing().parse( "{\"wait_for_active_shards\":\"1\"}" ) );
	}

	@Test
	void multipleAndAbsentBodiesExportAsArrays() throws IOException {
		ElasticsearchIndexSchemaExportImpl multiple = export(
				ElasticsearchJsonRequest.put().wholeEncodedPath( "/index" )
						.bodyJson( new ObjectNode( JsonNodeFactory.instance ) )
						.bodyJson( new ObjectNode( JsonNodeFactory.instance ) ).build() );
		multiple.toFiles( directory );
		assertThat( new JsonParsing().parse( Files.readString( directory.resolve( "create-index.json" ) ) ) )
				.isEqualTo( new JsonParsing().parse( "[{},{}]" ) );
		export( ElasticsearchJsonRequest.put().wholeEncodedPath( "/index" ).build() ).toFiles( directory );
		assertThat( Files.readString( directory.resolve( "create-index.json" ) ) ).isEqualTo( "[]" );
	}


	@Test
	void customObjectHandlerRunsAndDeprecatedGsonBodyIsIndependent() {
		ObjectNode body = (ObjectNode) new JsonParsing().parse( "{\"value\":9007199254740993,\"null\":null}" );
		ElasticsearchIndexSchemaExportImpl export = export( ElasticsearchJsonRequest.put().wholeEncodedPath( "/index" )
				.bodyJson( body ).build() );
		AtomicInteger reads = new AtomicInteger();
		JsonHandler<ObjectNode> handler =
				new JsonHandler<>() {
					@Override
					public Class<ObjectNode> javaType() {
						return ObjectNode.class;
					}

					@Override
					public String toJson(ObjectNode value) {
						throw new AssertionFailure( "Output handler only" );
					}

					@Override
					public ObjectNode fromJson(String json) {
						reads.incrementAndGet();
						return (ObjectNode) new JsonParsing().parse( json );
					}
				};
		ObjectNode converted = export.bodyParts( handler ).get( 0 );
		assertThat( reads.get() ).isEqualTo( 1 );
		assertThat( converted ).isEqualTo( body ).isNotSameAs( body );
		var gson = export.bodyParts().get( 0 );
		assertThat( gson.get( "value" ).getAsBigInteger() ).isEqualTo( new BigInteger( "9007199254740993" ) );
		assertThat( gson.get( "null" ).isJsonNull() ).isTrue();
		gson.addProperty( "value", 1 );
		assertThat( body.get( "value" ).longValue() ).isEqualTo( 9007199254740993L );
		assertThatThrownBy( () -> export.bodyParts().clear() ).isInstanceOf( UnsupportedOperationException.class );
	}

	private static ElasticsearchIndexSchemaExportImpl export(ElasticsearchJsonRequest request) {
		return new ElasticsearchIndexSchemaExportImpl( JsonProviderHelper.createUserFacingCodec(), "index", request );
	}
}
