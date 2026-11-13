package org.hibernate.search.backend.elasticsearch.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonTransportSupport;
import org.hibernate.search.backend.elasticsearch.client.common.json.impl.JsonResponseBody;
import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchJsonHandlerFieldCodec;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;

class ElasticsearchJsonParsingConfigurationTest {
	@Test
	void defaultsAndResponseBodyLimitValidation() {
		assertThat( options( Map.of() ) ).isEqualTo( JsonParsingOptions.DEFAULT );
		var empty = AllAwareConfigurationPropertySource.fromMap( Map.of() );
		assertThat( JsonTransportSupport.responseBodySizeLimit( empty ) ).isZero();
		var configured = AllAwareConfigurationPropertySource.fromMap( Map.of( "response_body_size_limit", "4294967296" ) );
		assertThat( JsonTransportSupport.responseBodySizeLimit( configured ) ).isEqualTo( 4294967296L );
		for ( Object invalid : new Object[] { -1, "invalid", Long.MIN_VALUE } ) {
			var source = AllAwareConfigurationPropertySource.fromMap( Map.of( "response_body_size_limit", invalid ) );
			assertThatThrownBy( () -> JsonTransportSupport.responseBodySizeLimit( source ) )
					.isInstanceOf( SearchException.class ).hasMessageContaining( "response_body_size_limit" );
		}
	}

	@ParameterizedTest
	@ValueSource(
			strings = { "json.max_nesting_depth", "json.max_string_length", "json.max_name_length", "json.max_number_length" })
	void invalidParserLimitReportsItsConfigurationKey(String key) {
		for ( Object invalid : new Object[] { 0, -1, "invalid", Long.MAX_VALUE } ) {
			assertThatThrownBy( () -> options( Map.of( key, invalid ) ) )
					.isInstanceOf( SearchException.class ).hasMessageContaining( key );
		}
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"json.max_nesting_depth|2|[[[0]]]",
			"json.max_string_length|3|{\"v\":\"abcdef\"}",
			"json.max_name_length|3|{\"éé\":0}",
			"json.max_number_length|3|123456" })
	void configuredConstraintsApplyToStringsStreamsResponsesAndFallbackImports(String key, int limit, String text)
			throws Exception {
		var validation = JsonValidation.of( false );
		var parsing = new JsonParsing( options( Map.of( key, limit ) ) );
		var provider = JsonProviderHelper.create( false, validation, parsing );
		var userCodec = JsonProviderHelper.createUserFacingCodec( validation, parsing );
		for ( var codec : new org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService[] {
				provider.getCodec(),
				provider.getCodecNoSerializeNulls(),
				userCodec } ) {
			assertThatThrownBy( () -> codec.decode( text, JsonNode.class ) ).isInstanceOf( RuntimeException.class );
			assertThatThrownBy( () -> codec.decode( new ByteArrayInputStream( text.getBytes( StandardCharsets.UTF_8 ) ),
					JsonNode.class ) ).isInstanceOf( RuntimeException.class );
		}
		try ( var response = new JsonResponseBody( StandardCharsets.UTF_8, 0, validation, parsing ) ) {
			response.feed( StandardCharsets.UTF_8.encode( text ) );
			response.complete();
			assertThatThrownBy( response::result ).isInstanceOf( RuntimeException.class );
		}
		var gson = GsonJsonHandlers.value().fromJson( text );
		assertThatThrownBy( () -> JsonHandlerBridge.importValue( GsonJsonHandlers.value(), gson, "test", parsing ) )
				.isInstanceOf( SearchException.class );
		var nativeCodec = new ElasticsearchJsonHandlerFieldCodec<>( userCodec, GsonJsonHandlers.value() );
		assertThatThrownBy( () -> nativeCodec.encode( gson ) ).isInstanceOf( SearchException.class );
		assertThat( JsonProviderHelper.create( false ).getCodec().decode( text, JsonNode.class ) ).isNotNull();
	}

	@Test
	void raisingNumberLimitDoesNotChangeOtherBackendsOrStandaloneHandlerDefaults() {
		String number = "1".repeat( JsonParsingOptions.DEFAULT.maxNumberLength() + 1 );
		var parsing =
				new JsonParsing( options( Map.of( ElasticsearchBackendSettings.JSON_MAX_NUMBER_LENGTH, number.length() ) ) );
		var codec = JsonProviderHelper.createUserFacingCodec( JsonValidation.of( false ), parsing );
		assertThat( codec.decode( number, JsonNode.class ).bigIntegerValue() ).isEqualTo( new java.math.BigInteger( number ) );
		var nativeCodec = new ElasticsearchJsonHandlerFieldCodec<>( codec, GsonJsonHandlers.value() );
		assertThat( nativeCodec.encode( GsonJsonHandlers.value().fromJson( number ) ).bigIntegerValue() )
				.isEqualTo( new java.math.BigInteger( number ) );
		assertThatThrownBy( () -> JsonProviderHelper.createUserFacingCodec().decode( number, JsonNode.class ) )
				.isInstanceOf( RuntimeException.class );
		assertThatThrownBy( () -> org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers.value()
				.fromJson( number ) ).isInstanceOf( RuntimeException.class );
	}

	@Test
	void independentBackendsApplyConstraintsToDslImportsWithoutChangingStandaloneHandlers() {
		var constrained = JsonProviderHelper.createUserFacingCodec( JsonValidation.of( false ),
				new JsonParsing( options( Map.of( ElasticsearchBackendSettings.JSON_MAX_NAME_LENGTH, 3 ) ) ) );
		var scope = mock( ElasticsearchSearchIndexScope.class );
		when( scope.userFacingCodec() ).thenReturn( constrained );
		String text = "{\"longname\":0}";
		var object = GsonJsonHandlers.object().fromJson( text );
		var predicates = new ElasticsearchSearchPredicateBuilderFactory( scope );
		var sorts = new ElasticsearchSearchSortBuilderFactory( scope );
		var aggregations = new ElasticsearchSearchAggregationBuilderFactory( scope );
		assertThatThrownBy( () -> predicates.fromJson( text ) ).isInstanceOf( SearchException.class );
		assertThatThrownBy( () -> predicates.fromJson( GsonJsonHandlers.object(), object ) )
				.isInstanceOf( SearchException.class );
		assertThatThrownBy( () -> sorts.fromJson( text ) ).isInstanceOf( SearchException.class );
		assertThatThrownBy( () -> sorts.fromJson( GsonJsonHandlers.object(), object ) ).isInstanceOf( SearchException.class );
		assertThatThrownBy( () -> aggregations.fromJson( text, GsonJsonHandlers.object() ) )
				.isInstanceOf( SearchException.class );
		assertThatThrownBy( () -> aggregations.fromJson( GsonJsonHandlers.object(), object ) )
				.isInstanceOf( SearchException.class );
		assertThat( org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers.object().fromJson( text ) )
				.isNotNull();
		assertThat( JsonProviderHelper.createUserFacingCodec().decode( text, JsonNode.class ) ).isNotNull();
	}

	private static JsonParsingOptions options(Map<String, ?> properties) {
		return ElasticsearchBackendFactory
				.resolveJsonParsingOptions( AllAwareConfigurationPropertySource.fromMap( properties ) );
	}
}
