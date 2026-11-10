package org.hibernate.search.backend.elasticsearch.gson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.ElasticsearchSearchAggregationFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeMappingStep;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ObjectNode;

@SuppressWarnings({ "deprecation", "unchecked" })
class GsonInputBridgeTest {
	@Test
	void deprecatedConveniencesSelectTheStableGsonHandlerWithoutEagerConversion() {
		var input = GsonJsonHandlers.object().fromJson( "{\"field\":{\"number\":9007199254740993}}" );
		var predicate = mock( ElasticsearchSearchPredicateFactory.class, CALLS_REAL_METHODS );
		predicate.fromJson( input );
		verify( predicate ).fromJson( GsonJsonHandlers.object(), input );

		var sort = mock( ElasticsearchSearchSortFactory.class, CALLS_REAL_METHODS );
		sort.fromJson( input );
		verify( sort ).fromJson( GsonJsonHandlers.object(), input );

		var aggregation = mock( ElasticsearchSearchAggregationFactory.class, CALLS_REAL_METHODS );
		aggregation.fromJson( input );
		verify( aggregation ).fromJson( GsonJsonHandlers.object(), input );
		aggregation.fromJson( "{}" );
		verify( aggregation ).fromJson( "{}", GsonJsonHandlers.object() );

		var mapping = mock( ElasticsearchNativeIndexFieldTypeMappingStep.class, CALLS_REAL_METHODS );
		mapping.mapping( input );
		verify( mapping ).mapping( GsonJsonHandlers.object(), input );
	}

	@Test
	void concretePredicateBuilderConvertsGsonAtTheBoundaryAndRetainsTrustedJacksonInput() {
		var scope = mock( ElasticsearchSearchIndexScope.class );
		var builder = new ElasticsearchSearchPredicateBuilderFactory( scope );
		var input = GsonJsonHandlers.object().fromJson( "{\"field\":{\"number\":9007199254740993,\"nested\":null}}" );
		var converted = builder.fromJson( GsonJsonHandlers.object(), input ).toJsonQuery( null );
		assertThat( converted.path( "field" ).path( "number" ).longValue() ).isEqualTo( 9007199254740993L );
		assertThat( converted.path( "field" ).path( "nested" ).isNull() ).isTrue();
		( (ObjectNode) converted.get( "field" ) ).put( "changed", true );
		assertThat( input.getAsJsonObject( "field" ).has( "changed" ) ).isFalse();
		assertThat( builder.fromJson( JacksonJsonHandlers.object(), converted ).toJsonQuery( null ) ).isSameAs( converted );
	}
}
