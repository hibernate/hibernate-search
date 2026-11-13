package org.hibernate.search.backend.elasticsearch.json.internal.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.Elasticsearch7SearchSyntax;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class JsonCodecServiceTest {
	private final JsonCodecService codec = JsonProviderHelper.create( false ).getCodecNoSerializeNulls();

	@Test
	void wrongContainerTypesHaveTypedDecodeErrors() {
		assertThatThrownBy( () -> codec.decode( "[]", ObjectNode.class ) )
				.isInstanceOf( JsonParseException.class ).hasMessageContaining( ObjectNode.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class );
		assertThatThrownBy( () -> codec.decode( "{}", ArrayNode.class ) )
				.isInstanceOf( JsonParseException.class ).hasMessageContaining( ArrayNode.class.getName() )
				.hasCauseInstanceOf( ClassCastException.class );
	}

	@Test
	void byteStreamsUseBoundedReadsAndPreserveExplicitCharsets() throws Exception {
		String json = "{\"extra\":\"" + "é中".repeat( 20000 ) + "\"}";
		var bytes = json.getBytes( StandardCharsets.UTF_16LE );
		var input = new ByteArrayInputStream( bytes ) {
			@Override
			public synchronized int read(byte[] buffer, int offset, int count) {
				assertThat( count ).isLessThanOrEqualTo( 8192 );
				return super.read( buffer, offset, Math.min( count, 7 ) );
			}
		};
		IndexSettings value = codec.decode( input, StandardCharsets.UTF_16LE, IndexSettings.class );
		assertThat( value.getExtraAttributes().get( "extra" ).asString() ).isEqualTo( "é中".repeat( 20000 ) );
		assertThat( codec.decode( new ByteArrayInputStream( new byte[0] ), ObjectNode.class ) ).isNull();
		assertThat( codec.decode( new ByteArrayInputStream( " \n\t".getBytes( StandardCharsets.UTF_8 ) ), ObjectNode.class ) )
				.isNull();
		assertThatThrownBy( () -> codec.decode( new ByteArrayInputStream( "{\"a\":".getBytes( StandardCharsets.UTF_8 ) ),
				ObjectNode.class ) ).isInstanceOf( JsonParseException.class );
	}

	@Test
	void existingTreesAreDecodedAndCopiedStructurallyWithRecursiveNullPolicy() {
		var tree = codec.decode( "{\"analysis\":{\"custom\":{\"null\":null,\"array\":[null,{\"null\":null}]}},\"null\":null}",
				ObjectNode.class );
		IndexSettings value = codec.decode( tree, IndexSettings.class );
		var copy = codec.decode( codec.encode( value ), IndexSettings.class );
		( (ObjectNode) copy.getAnalysis().getExtraAttributes().get( "custom" ) ).put( "changed", true );
		assertThat( value.getAnalysis().getExtraAttributes().get( "custom" ).has( "changed" ) ).isFalse();
		assertThat( codec.encode( value ).has( "null" ) ).isFalse();
		var extra = codec.encode( value ).get( "analysis" ).get( "custom" );
		assertThat( extra.has( "null" ) ).isFalse();
		assertThat( extra.get( "array" ).get( 0 ).isNull() ).isTrue();
		assertThat( extra.get( "array" ).get( 1 ).has( "null" ) ).isFalse();
		var withNulls = JsonProviderHelper.create( false ).getCodec().encode( value );
		assertThat( withNulls.get( "null" ).isNull() ).isTrue();
		assertThat( withNulls.get( "analysis" ).get( "custom" ).get( "null" ).isNull() ).isTrue();

	}

	@Test
	void recursiveRegisteredCodecsCanInitializeAndRunConcurrently() throws Exception {
		try ( var workers = Executors.newFixedThreadPool( 8 ) ) {
			var jobs = new ArrayList<Callable<String>>();
			for ( int i = 0; i < 100; i++ ) {
				jobs.add( () -> {
					var service = JsonProviderHelper.create( false ).getCodecNoSerializeNulls();
					var value =
							service.decode( "{\"properties\":{\"parent\":{\"properties\":{\"leaf\":{\"type\":\"keyword\"}}}}}",
									RootTypeMapping.class );
					return service.write( value );
				} );
			}
			for ( var result : workers.invokeAll( jobs ) ) {
				assertThat( result.get() ).contains( "keyword" );
			}
		}
	}

	@Test
	void longSearchSyntaxKeepsStringSerializationAndNull() {
		var syntax = new Elasticsearch7SearchSyntax();
		assertThat( syntax.encodeLongForAggregation( 9007199254740993L ).isString() ).isTrue();
		assertThat( syntax.encodeLongForAggregation( 9007199254740993L ).asString() ).isEqualTo( "9007199254740993" );
		assertThat( syntax.encodeLongForAggregation( null ).isNull() ).isTrue();
	}
}
