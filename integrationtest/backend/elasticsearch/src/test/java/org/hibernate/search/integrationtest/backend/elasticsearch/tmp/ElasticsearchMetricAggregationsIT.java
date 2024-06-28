/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.tmp;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ElasticsearchMetricAggregationsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final AggregationKey<Integer> sumIntegers = AggregationKey.of( "sumIntegers" );
	private final AggregationKey<String> sumConverted = AggregationKey.of( "sumConverted" );
	private final AggregationKey<Integer> sumConvertedNoConversion = AggregationKey.of( "sumConvertedNoConversion" );
	private final AggregationKey<Integer> sumFiltered = AggregationKey.of( "sumFiltered" );
	private final AggregationKey<Integer> minIntegers = AggregationKey.of( "minIntegers" );
	private final AggregationKey<String> minConverted = AggregationKey.of( "minConverted" );
	private final AggregationKey<Integer> maxIntegers = AggregationKey.of( "maxIntegers" );
	private final AggregationKey<String> maxConverted = AggregationKey.of( "maxConverted" );
	private final AggregationKey<Long> countIntegers = AggregationKey.of( "countIntegers" );
	private final AggregationKey<Long> countConverted = AggregationKey.of( "countConverted" );
	private final AggregationKey<Long> countDistinctIntegers = AggregationKey.of( "countDistinctIntegers" );
	private final AggregationKey<Long> countDistinctConverted = AggregationKey.of( "countDistinctConverted" );
	private final AggregationKey<Integer> avgIntegers = AggregationKey.of( "avgIntegers" );
	private final AggregationKey<String> avgConverted = AggregationKey.of( "avgConverted" );
	private final AggregationKey<Double> avgIntegersAsDouble = AggregationKey.of( "avgIntegersAsDouble" );
	private final AggregationKey<Double> avgIntegersAsDoubleFiltered = AggregationKey.of( "avgIntegersAsDoubleFiltered" );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndexes( mainIndex ).setup().integration();
		initData();
	}

	@Test
	public void test_filteringResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "style" ).matching( "bla" ) )
				.aggregation( sumIntegers, f -> f.sum().field( "integer", Integer.class ) )
				.aggregation( sumConverted, f -> f.sum().field( "converted", String.class ) )
				.aggregation( sumConvertedNoConversion, f -> f.sum().field( "converted", Integer.class, ValueConvert.NO ) )
				.aggregation( sumFiltered, f -> f.sum().field( "object.nestedInteger", Integer.class )
						.filter( ff -> ff.range().field( "object.nestedInteger" ).atLeast( 5 ) ) )
				.aggregation( minIntegers, f -> f.min().field( "integer", Integer.class ) )
				.aggregation( minConverted, f -> f.min().field( "converted", String.class ) )
				.aggregation( maxIntegers, f -> f.max().field( "integer", Integer.class ) )
				.aggregation( maxConverted, f -> f.max().field( "converted", String.class ) )
				.aggregation( countIntegers, f -> f.count().field( "integer" ) )
				.aggregation( countConverted, f -> f.count().field( "converted" ) )
				.aggregation( countDistinctIntegers, f -> f.countDistinct().field( "integer" ) )
				.aggregation( countDistinctConverted, f -> f.countDistinct().field( "converted" ) )
				.aggregation( avgIntegers, f -> f.avg().field( "integer", Integer.class ) )
				.aggregation( avgConverted, f -> f.avg().field( "converted", String.class ) )
				.aggregation( avgIntegersAsDouble, f -> f.avg().field( "integer", Double.class ) )
				.aggregation( avgIntegersAsDoubleFiltered, f -> f.avg().field( "object.nestedInteger", Double.class )
						.filter( ff -> ff.range().field( "object.nestedInteger" ).atLeast( 5 ) ) )
				.toQuery();

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( sumIntegers ) ).isEqualTo( 29 );
		assertThat( result.aggregation( sumConverted ) ).isEqualTo( "29" );
		assertThat( result.aggregation( sumConvertedNoConversion ) ).isEqualTo( 29 );
		assertThat( result.aggregation( sumFiltered ) ).isEqualTo( 23 );
		assertThat( result.aggregation( minIntegers ) ).isEqualTo( 3 );
		assertThat( result.aggregation( minConverted ) ).isEqualTo( "3" );
		assertThat( result.aggregation( maxIntegers ) ).isEqualTo( 9 );
		assertThat( result.aggregation( maxConverted ) ).isEqualTo( "9" );
		assertThat( result.aggregation( countIntegers ) ).isEqualTo( 5 );
		assertThat( result.aggregation( countConverted ) ).isEqualTo( 5 );
		assertThat( result.aggregation( countDistinctIntegers ) ).isEqualTo( 3 );
		assertThat( result.aggregation( countDistinctConverted ) ).isEqualTo( 3 );
		assertThat( result.aggregation( avgIntegers ) ).isEqualTo( 5 );
		assertThat( result.aggregation( avgConverted ) ).isEqualTo( "5" );
		assertThat( result.aggregation( avgIntegersAsDouble ) ).isEqualTo( 5.8 );
		assertThat( result.aggregation( avgIntegersAsDoubleFiltered ) ).isEqualTo( 7.666666666666667 );
	}

	@Test
	public void test_allResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.aggregation( sumIntegers, f -> f.sum().field( "integer", Integer.class ) )
				.aggregation( sumConverted, f -> f.sum().field( "converted", String.class ) )
				.aggregation( sumConvertedNoConversion, f -> f.sum().field( "converted", Integer.class, ValueConvert.NO ) )
				.aggregation( sumFiltered, f -> f.sum().field( "object.nestedInteger", Integer.class )
						.filter( ff -> ff.range().field( "object.nestedInteger" ).atLeast( 5 ) ) )
				.aggregation( minIntegers, f -> f.min().field( "integer", Integer.class ) )
				.aggregation( minConverted, f -> f.min().field( "converted", String.class ) )
				.aggregation( maxIntegers, f -> f.max().field( "integer", Integer.class ) )
				.aggregation( maxConverted, f -> f.max().field( "converted", String.class ) )
				.aggregation( countIntegers, f -> f.count().field( "integer" ) )
				.aggregation( countConverted, f -> f.count().field( "converted" ) )
				.aggregation( countDistinctIntegers, f -> f.countDistinct().field( "integer" ) )
				.aggregation( countDistinctConverted, f -> f.countDistinct().field( "converted" ) )
				.aggregation( avgIntegers, f -> f.avg().field( "integer", Integer.class ) )
				.aggregation( avgConverted, f -> f.avg().field( "converted", String.class ) )
				.aggregation( avgIntegersAsDouble, f -> f.avg().field( "integer", Double.class ) )
				.aggregation( avgIntegersAsDoubleFiltered, f -> f.avg().field( "object.nestedInteger", Double.class )
						.filter( ff -> ff.range().field( "object.nestedInteger" ).atLeast( 5 ) ) )
				.toQuery();

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( sumIntegers ) ).isEqualTo( 55 );
		assertThat( result.aggregation( sumConverted ) ).isEqualTo( "55" );
		assertThat( result.aggregation( sumConvertedNoConversion ) ).isEqualTo( 55 );
		assertThat( result.aggregation( sumFiltered ) ).isEqualTo( 59 );
		assertThat( result.aggregation( minIntegers ) ).isEqualTo( -10 );
		assertThat( result.aggregation( minConverted ) ).isEqualTo( "-10" );
		assertThat( result.aggregation( maxIntegers ) ).isEqualTo( 18 );
		assertThat( result.aggregation( maxConverted ) ).isEqualTo( "18" );
		assertThat( result.aggregation( countIntegers ) ).isEqualTo( 10 );
		assertThat( result.aggregation( countConverted ) ).isEqualTo( 10 );
		assertThat( result.aggregation( countDistinctIntegers ) ).isEqualTo( 6 );
		assertThat( result.aggregation( countDistinctConverted ) ).isEqualTo( 6 );
		assertThat( result.aggregation( avgIntegers ) ).isEqualTo( 5 );
		assertThat( result.aggregation( avgConverted ) ).isEqualTo( "5" );
		assertThat( result.aggregation( avgIntegersAsDouble ) ).isEqualTo( 5.5 );
		assertThat( result.aggregation( avgIntegersAsDoubleFiltered ) ).isEqualTo( 11.8 );
	}

	private void initData() {
		int[] integers = new int[] { 9, 18, 3, 18, 7, -10, 3, 0, 7, 0 };
		String[] styles = new String[] { "bla", "aaa" };

		BulkIndexer bulkIndexer = mainIndex.bulkIndexer();
		for ( int i = 0; i < integers.length; i++ ) {
			int value = integers[i];
			String style = styles[i % 2];
			String id = i + ":" + value + ":" + style;

			bulkIndexer.add( id, document -> {
				document.addValue( mainIndex.binding().integer, value );
				document.addValue( mainIndex.binding().converted, value );
				document.addValue( mainIndex.binding().style, style );

				DocumentElement object = document.addObject( mainIndex.binding().object );
				object.addValue( mainIndex.binding().nestedInteger, value );
			} );
		}
		bulkIndexer.add( "empty", document -> {} )
				.join();
	}

	@SuppressWarnings("unused")
	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<Integer> converted;
		final IndexFieldReference<String> style;
		final IndexObjectFieldReference object;
		final IndexFieldReference<Integer> nestedInteger;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().aggregable( Aggregable.YES ) ).toReference();
			converted = root.field( "converted", f -> f.asInteger().aggregable( Aggregable.YES )
					.projectionConverter( String.class, (value, context) -> value.toString() ) ).toReference();
			style = root.field( "style", f -> f.asString() ).toReference();

			IndexSchemaObjectField nested = root.objectField( "object", ObjectStructure.NESTED );
			object = nested.toReference();
			nestedInteger = nested.field( "nestedInteger", f -> f.asInteger().aggregable( Aggregable.YES ) )
					.toReference();
		}
	}
}
