package org.hibernate.search.backend.elasticsearch.json.internal.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.ElasticsearchDenseVectorIndexOptions;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.OpenSearchVectorTypeMethod;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonCodecCharacterizationTest {

	private final JsonCodecService codec = JsonProviderHelper.create(
			false )
			.getCodecNoSerializeNulls();

	static Stream<Arguments> codecs() {
		return Stream.of(
				Arguments.of( IndexAliasDefinition.class, "{\"is_write_index\":true,\"filter\":{\"term\":{\"tenant\":1}}}" ),
				Arguments.of( AnalyzerDefinition.class,
						"{\"type\":\"custom\",\"tokenizer\":\"standard\",\"filter\":[\"lowercase\"],\"custom\":1}" ),
				Arguments.of( CharFilterDefinition.class, "{\"type\":\"mapping\",\"mappings\":[\"a => b\"]}" ),
				Arguments.of( NormalizerDefinition.class, "{\"type\":\"custom\",\"filter\":[\"lowercase\"]}" ),
				Arguments.of( TokenFilterDefinition.class, "{\"type\":\"stop\",\"stopwords\":[\"a\",\"the\"]}" ),
				Arguments.of( TokenizerDefinition.class, "{\"type\":\"ngram\",\"min_gram\":2}" ),
				Arguments.of( Analysis.class,
						"{\"analyzer\":{\"a\":{\"type\":\"custom\",\"tokenizer\":\"standard\"}},\"custom\":true}" ),
				Arguments.of( IndexSettings.class,
						"{\"max_result_window\":20000,\"knn\":true,\"number_of_shards\":2,\"analysis\":{\"filter\":{\"f\":{\"type\":\"stop\"}}}}" ),
				Arguments.of( DynamicTemplate.class,
						"{\"match\":\"*_text\",\"match_mapping_type\":\"string\",\"mapping\":{\"type\":\"text\"}}" ),
				Arguments.of( ElasticsearchDenseVectorIndexOptions.class,
						"{\"type\":\"hnsw\",\"m\":16,\"ef_construction\":100,\"custom\":true}" ),
				Arguments.of( OpenSearchVectorTypeMethod.class,
						"{\"name\":\"hnsw\",\"engine\":\"lucene\",\"space_type\":\"l2\",\"parameters\":{\"ef_construction\":100,\"m\":16,\"custom\":1}}" ),
				Arguments.of( OpenSearchVectorTypeMethod.Parameters.class, "{\"ef_construction\":100,\"m\":16,\"custom\":1}" ),
				Arguments.of( PropertyMapping.class,
						"{\"type\":\"date\",\"format\":\"strict_date_optional_time||epoch_millis\",\"null_value\":9007199254740993,\"doc_values\":false,\"custom\":{\"nested\":3}}" ),
				Arguments.of( PropertyMapping.class, "{\"type\":\"scaled_float\",\"scaling_factor\":100.0}" ),
				Arguments.of( RootTypeMapping.class,
						"{\"dynamic\":\"strict\",\"_routing\":{\"required\":true},\"properties\":{\"name\":{\"type\":\"keyword\"}},\"dynamic_templates\":[{\"strings\":{\"match_mapping_type\":\"string\",\"mapping\":{\"type\":\"keyword\"}}}]}" ),
				Arguments.of( NamedDynamicTemplate.class,
						"{\"strings\":{\"match\":\"*\",\"mapping\":{\"type\":\"keyword\"}}}" ),
				Arguments.of( DynamicType.class, "\"true\"" ),
				Arguments.of( DynamicType.class, "\"false\"" ),
				Arguments.of( DynamicType.class, "\"strict\"" ),
				Arguments.of( RoutingType.class, "{\"required\":true}" )
		);
	}

	@ParameterizedTest(name = "{0}: {1}")
	@MethodSource("codecs")
	void roundTrip(Class<?> type, String json) {
		assertThat( codec.getCodec( type ).getClass().getName() ).doesNotContain( "ReflectiveTypeAdapter" );
		Object value = codec.decode( json, type );
		assertThat( JsonScalarConversions.equivalent( codec.encode( value ), new JsonParsing().parse( json ) ) ).isTrue();
		assertThat( codec.decode( "null", type ) ).isNull();
		assertThat( codec.write( null ) ).isEqualTo( "null" );
	}

	@Test
	void handwrittenRepresentationsAndFailures() {
		// Optional routing reads successfully, but its adapter cannot write OPTIONAL.
		assertThat( codec.decode( "{\"required\":false}", RoutingType.class ) ).isEqualTo( RoutingType.OPTIONAL );
		assertThatThrownBy( () -> codec.write( RoutingType.OPTIONAL ) ).isInstanceOf( AssertionFailure.class );
		assertThat( codec.decode( "{}", RoutingType.class ) ).isNull();
		assertThatThrownBy( () -> codec.decode( "{\"unknown\":true}", RoutingType.class ) )
				.isInstanceOf( AssertionFailure.class );
		assertThatThrownBy( () -> codec.decode( "true", DynamicType.class ) ).isInstanceOf( RuntimeException.class );
		assertThatThrownBy( () -> codec.decode( "\"unknown\"", DynamicType.class ) ).isInstanceOf( AssertionFailure.class );
		assertThatThrownBy( () -> codec.decode( "{}", NamedDynamicTemplate.class ) ).isInstanceOf( RuntimeException.class );
		assertThatThrownBy( () -> codec.decode( "{\"a\":{},\"b\":{}}", NamedDynamicTemplate.class ) )
				.isInstanceOf( RuntimeException.class );
	}

	@Test
	void formatAndNullSerialization() {
		PropertyMapping mapping =
				codec.decode( "{\"type\":\"date\",\"format\":\"a||b\",\"unknown\":null}", PropertyMapping.class );
		assertThat( mapping.getFormat() ).containsExactly( "a", "b" );
		assertThat( codec.encode( mapping ).has( "unknown" ) ).isFalse();
		JsonCodecService withNulls = JsonProviderHelper.create( false ).getCodec();
		assertThat( withNulls.encode( mapping ).get( "unknown" ).isNull() ).isTrue();
		assertThat( withNulls.encode( mapping ).get( "index" ).isNull() ).isTrue();
		assertThat( codec.decode( "{\"format\":null}", PropertyMapping.class ).getFormat() ).isNull();
		assertThat( codec.decode( "{\"format\":\"a||b||\"}", PropertyMapping.class ).getFormat() )
				.containsExactly( "a", "b" );
		assertThatThrownBy( () -> codec.decode( "{\"format\":[\"a\",\"b\"]}", PropertyMapping.class ) )
				.isInstanceOf( RuntimeException.class );
	}

	@Test
	void generatedMemberOrderAndExtraPropertyOrder() {
		IndexSettings settings =
				codec.decode( "{\"z\":1,\"max_result_window\":20,\"a\":2,\"knn\":true}", IndexSettings.class );
		assertThat( codec.encode( settings ).propertyNames() )
				.containsExactly( "knn", "max_result_window", "z", "a" );
	}

	@Test
	void codecsPreserveLargeNumericValues() {
		PropertyMapping mapping = codec.decode( "{\"null_value\":9007199254740993}", PropertyMapping.class );
		assertThat( mapping.getNullValue().longValue() ).isEqualTo( 9007199254740993L );
		assertThat( codec.encode( mapping ).get( "null_value" ).toString() )
				.isEqualTo( "9007199254740993" );
		IndexSettings settings = codec.decode( "{\"extra\":1.2300e+04}", IndexSettings.class );
		assertThat( codec.encode( settings ).get( "extra" ).decimalValue() )
				.isEqualByComparingTo( "12300" );
	}

	@Test
	void unregisteredTypesHaveNoFallback() {
		assertThatThrownBy( () -> codec.decode( "{}", Unknown.class ) ).isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "No JSON codec registered" );
		assertThatThrownBy( () -> codec.encode( new Unknown() ) ).isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "No JSON codec registered" );
	}

	private static final class Unknown { public String value; }
}
