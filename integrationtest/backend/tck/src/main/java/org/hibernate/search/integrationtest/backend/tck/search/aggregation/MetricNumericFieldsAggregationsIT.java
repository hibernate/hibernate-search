/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MetricNumericFieldsAggregationsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final AggregationKey<Integer> sumIntegers = AggregationKey.of( "sumIntegers" );
	private final AggregationKey<String> sumIntegersAsString = AggregationKey.of( "sumIntegersAsString" );
	private final AggregationKey<Object> sumIntegersRaw = AggregationKey.of( "sumIntegersRaw" );
	private final AggregationKey<String> sumConverted = AggregationKey.of( "sumConverted" );
	private final AggregationKey<Integer> sumConvertedNoConversion = AggregationKey.of( "sumConvertedNoConversion" );
	private final AggregationKey<Integer> sumFiltered = AggregationKey.of( "sumFiltered" );
	private final AggregationKey<Integer> minIntegers = AggregationKey.of( "minIntegers" );
	private final AggregationKey<String> minIntegersAsString = AggregationKey.of( "minIntegersAsString" );
	private final AggregationKey<String> minConverted = AggregationKey.of( "minConverted" );
	private final AggregationKey<Integer> maxIntegers = AggregationKey.of( "maxIntegers" );
	private final AggregationKey<String> maxIntegersAsString = AggregationKey.of( "maxIntegersAsString" );
	private final AggregationKey<String> maxConverted = AggregationKey.of( "maxConverted" );
	private final AggregationKey<Long> countIntegers = AggregationKey.of( "countIntegers" );
	private final AggregationKey<Long> countConverted = AggregationKey.of( "countConverted" );
	private final AggregationKey<Long> countDistinctIntegers = AggregationKey.of( "countDistinctIntegers" );
	private final AggregationKey<Long> countDistinctConverted = AggregationKey.of( "countDistinctConverted" );
	private final AggregationKey<Integer> avgIntegers = AggregationKey.of( "avgIntegers" );
	private final AggregationKey<String> avgIntegersAsString = AggregationKey.of( "avgIntegersAsString" );
	private final AggregationKey<String> avgConverted = AggregationKey.of( "avgConverted" );
	private final AggregationKey<Double> avgIntegersAsDouble = AggregationKey.of( "avgIntegersAsDouble" );
	private final AggregationKey<Double> avgIntegersAsDoubleRaw = AggregationKey.of( "avgIntegersAsDoubleRaw" );
	private final AggregationKey<Double> avgIntegersAsDoubleFiltered = AggregationKey.of( "avgIntegersAsDoubleFiltered" );
	private final AggregationKey<Double> sumDoubles = AggregationKey.of( "sumDoubles" );
	private final AggregationKey<Double> sumDoublesRaw = AggregationKey.of( "sumDoublesRaw" );
	private final AggregationKey<Float> sumFloats = AggregationKey.of( "sumFloats" );
	private final AggregationKey<BigInteger> sumBigIntegers = AggregationKey.of( "sumBigIntegers" );
	private final AggregationKey<BigDecimal> sumBigDecimals = AggregationKey.of( "sumBigDecimals" );
	private final AggregationKey<Double> avgDoubles = AggregationKey.of( "avgDoubles" );
	private final AggregationKey<Float> avgFloats = AggregationKey.of( "avgFloats" );
	private final AggregationKey<BigInteger> avgBigIntegers = AggregationKey.of( "avgBigIntegers" );
	private final AggregationKey<BigDecimal> avgBigDecimals = AggregationKey.of( "avgBigDecimals" );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndexes( mainIndex ).setup().integration();
		initData();
	}

	@Test
	void test_filteringResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQueryOptionsStep<?, ?, DocumentReference, StubLoadingOptionsStep, ?, ?> options = scope.query()
				.where( f -> f.match().field( "style" ).matching( "bla" ) );
		SearchQuery<DocumentReference> query = defineAggregations( options );

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( sumIntegers ) ).isEqualTo( 29 );
		assertThat( result.aggregation( sumIntegersAsString ) ).isEqualTo( "29" );
		assertThat( result.aggregation( sumConverted ) ).isEqualTo( "29" );
		assertThat( result.aggregation( sumConvertedNoConversion ) ).isEqualTo( 29 );
		assertThat( result.aggregation( sumFiltered ) ).isEqualTo( 23 );
		assertThat( result.aggregation( minIntegers ) ).isEqualTo( 3 );
		assertThat( result.aggregation( minIntegersAsString ) ).isEqualTo( "3" );
		assertThat( result.aggregation( minConverted ) ).isEqualTo( "3" );
		assertThat( result.aggregation( maxIntegers ) ).isEqualTo( 9 );
		assertThat( result.aggregation( maxIntegersAsString ) ).isEqualTo( "9" );
		assertThat( result.aggregation( maxConverted ) ).isEqualTo( "9" );
		assertThat( result.aggregation( countIntegers ) ).isEqualTo( 5 );
		assertThat( result.aggregation( countConverted ) ).isEqualTo( 5 );
		assertThat( result.aggregation( countDistinctIntegers ) ).isEqualTo( 3 );
		assertThat( result.aggregation( countDistinctConverted ) ).isEqualTo( 3 );
		assertThat( result.aggregation( avgIntegers ) ).isEqualTo( 5 );
		assertThat( result.aggregation( avgIntegersAsString ) ).isEqualTo( "5" );
		assertThat( result.aggregation( avgConverted ) ).isEqualTo( "5" );
		assertThat( result.aggregation( avgIntegersAsDouble ) ).isEqualTo( 5.8 );
		assertThat( result.aggregation( avgIntegersAsDoubleRaw ) ).isEqualTo( 5.8 );
		assertThat( result.aggregation( avgIntegersAsDoubleFiltered ) ).isEqualTo( 7.666666666666667 );
		assertThat( result.aggregation( sumDoubles ) ).isEqualTo( 29.0 );
		assertThat( result.aggregation( sumDoublesRaw ) ).isEqualTo( 29.0 );
		assertThat( result.aggregation( sumFloats ) ).isEqualTo( 29F );
		assertThat( result.aggregation( sumBigIntegers ) ).isEqualTo( BigInteger.valueOf( 29 ) );
		assertThat( result.aggregation( sumBigDecimals ).setScale( 2, RoundingMode.CEILING ) )
				.isEqualTo( BigDecimal.valueOf( 2900, 2 ) );
		assertThat( result.aggregation( avgDoubles ) ).isEqualTo( 5.8 );
		assertThat( result.aggregation( avgFloats ) ).isEqualTo( 5.8F );
		assertThat( result.aggregation( avgBigIntegers ) ).isEqualTo( BigInteger.valueOf( 5 ) );
		assertThat( result.aggregation( avgBigDecimals ).setScale( 2, RoundingMode.CEILING ) )
				.isEqualTo( BigDecimal.valueOf( 580, 2 ) );
	}

	@Test
	void test_allResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQueryOptionsStep<?, ?, DocumentReference, StubLoadingOptionsStep, ?, ?> options = scope.query()
				.where( f -> f.matchAll() );
		SearchQuery<DocumentReference> query = defineAggregations( options );

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( sumIntegers ) ).isEqualTo( 55 );
		assertThat( result.aggregation( sumIntegersAsString ) ).isEqualTo( "55" );
		assertThat( result.aggregation( sumConverted ) ).isEqualTo( "55" );
		assertThat( result.aggregation( sumConvertedNoConversion ) ).isEqualTo( 55 );
		assertThat( result.aggregation( sumFiltered ) ).isEqualTo( 59 );
		assertThat( result.aggregation( minIntegers ) ).isEqualTo( -10 );
		assertThat( result.aggregation( minIntegersAsString ) ).isEqualTo( "-10" );
		assertThat( result.aggregation( minConverted ) ).isEqualTo( "-10" );
		assertThat( result.aggregation( maxIntegers ) ).isEqualTo( 18 );
		assertThat( result.aggregation( maxIntegersAsString ) ).isEqualTo( "18" );
		assertThat( result.aggregation( maxConverted ) ).isEqualTo( "18" );
		assertThat( result.aggregation( countIntegers ) ).isEqualTo( 10 );
		assertThat( result.aggregation( countConverted ) ).isEqualTo( 10 );
		assertThat( result.aggregation( countDistinctIntegers ) ).isEqualTo( 6 );
		assertThat( result.aggregation( countDistinctConverted ) ).isEqualTo( 6 );
		assertThat( result.aggregation( avgIntegers ) ).isEqualTo( 5 );
		assertThat( result.aggregation( avgIntegersAsString ) ).isEqualTo( "5" );
		assertThat( result.aggregation( avgConverted ) ).isEqualTo( "5" );
		assertThat( result.aggregation( avgIntegersAsDouble ) ).isEqualTo( 5.5 );
		assertThat( result.aggregation( avgIntegersAsDoubleRaw ) ).isEqualTo( 5.5 );
		assertThat( result.aggregation( avgIntegersAsDoubleFiltered ) ).isEqualTo( 11.8 );
		assertThat( result.aggregation( sumDoubles ) ).isEqualTo( 55.0 );
		assertThat( result.aggregation( sumDoublesRaw ) ).isEqualTo( 55.0 );
		assertThat( result.aggregation( sumFloats ) ).isEqualTo( 55F );
		assertThat( result.aggregation( sumBigIntegers ) ).isEqualTo( BigInteger.valueOf( 55 ) );
		assertThat( result.aggregation( sumBigDecimals ).setScale( 2, RoundingMode.CEILING ) )
				.isEqualTo( BigDecimal.valueOf( 5500, 2 ) );
		assertThat( result.aggregation( avgDoubles ) ).isEqualTo( 5.5 );
		assertThat( result.aggregation( avgFloats ) ).isEqualTo( 5.5F );
		assertThat( result.aggregation( avgBigIntegers ) ).isEqualTo( BigInteger.valueOf( 5 ) );
		assertThat( result.aggregation( avgBigDecimals ).setScale( 2, RoundingMode.CEILING ) )
				.isEqualTo( BigDecimal.valueOf( 550, 2 ) );
	}

	private SearchQuery<DocumentReference> defineAggregations(
			SearchQueryOptionsStep<?, ?, DocumentReference, StubLoadingOptionsStep, ?, ?> options) {

		options.aggregation( sumIntegersRaw, f -> f.sum().field( "integer", Object.class, ValueModel.RAW ) );

		return options
				.aggregation( sumIntegers, f -> f.sum().field( "integer", Integer.class ) )
				.aggregation( sumIntegersAsString, f -> f.sum().field( "integer", String.class, ValueModel.STRING ) )
				.aggregation( sumConverted, f -> f.sum().field( "converted", String.class ) )
				.aggregation(
						sumConvertedNoConversion, f -> f.sum().field( "converted", Integer.class, ValueModel.INDEX ) )
				.aggregation( sumFiltered, f -> f.sum().field( "object.nestedInteger", Integer.class )
						.filter( ff -> ff.range().field( "object.nestedInteger" ).atLeast( 5 ) ) )
				.aggregation( minIntegers, f -> f.min().field( "integer", Integer.class ) )
				.aggregation( minIntegersAsString, f -> f.min().field( "integer", String.class, ValueModel.STRING ) )
				.aggregation( minConverted, f -> f.min().field( "converted", String.class ) )
				.aggregation( maxIntegers, f -> f.max().field( "integer", Integer.class ) )
				.aggregation( maxIntegersAsString, f -> f.max().field( "integer", String.class, ValueModel.STRING ) )
				.aggregation( maxConverted, f -> f.max().field( "converted", String.class ) )
				.aggregation( countIntegers, f -> f.count().field( "integer" ) )
				.aggregation( countConverted, f -> f.count().field( "converted" ) )
				.aggregation( countDistinctIntegers, f -> f.countDistinct().field( "integer" ) )
				.aggregation( countDistinctConverted, f -> f.countDistinct().field( "converted" ) )
				.aggregation( avgIntegers, f -> f.avg().field( "integer", Integer.class ) )
				.aggregation( avgIntegersAsString, f -> f.avg().field( "integer", String.class, ValueModel.STRING ) )
				.aggregation( avgConverted, f -> f.avg().field( "converted", String.class ) )
				.aggregation( avgIntegersAsDouble, f -> f.avg().field( "integer", Double.class, ValueModel.RAW ) )
				.aggregation( avgIntegersAsDoubleRaw, f -> f.avg().field( "integer", Double.class, ValueModel.RAW ) )
				.aggregation( avgIntegersAsDoubleFiltered,
						f -> f.avg().field( "object.nestedInteger", Double.class, ValueModel.RAW )
								.filter( ff -> ff.range().field( "object.nestedInteger" ).atLeast( 5 ) ) )
				.aggregation( sumDoubles, f -> f.sum().field( "doubleF", Double.class ) )
				.aggregation( sumDoublesRaw, f -> f.sum().field( "doubleF", Double.class, ValueModel.RAW ) )
				.aggregation( sumFloats, f -> f.sum().field( "floatF", Float.class ) )
				.aggregation( sumBigIntegers, f -> f.sum().field( "bigInteger", BigInteger.class ) )
				.aggregation( sumBigDecimals, f -> f.sum().field( "bigDecimal", BigDecimal.class ) )
				.aggregation( avgDoubles, f -> f.avg().field( "doubleF", Double.class ) )
				.aggregation( avgFloats, f -> f.avg().field( "floatF", Float.class ) )
				.aggregation( avgBigIntegers, f -> f.avg().field( "bigInteger", BigInteger.class ) )
				.aggregation( avgBigDecimals, f -> f.avg().field( "bigDecimal", BigDecimal.class ) )
				.toQuery();
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
				document.addValue( mainIndex.binding().doubleF, (double) value );
				document.addValue( mainIndex.binding().floatF, (float) value );
				document.addValue( mainIndex.binding().bigInteger, BigInteger.valueOf( value ) );
				document.addValue( mainIndex.binding().bigDecimal, BigDecimal.valueOf( value ) );
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
		final IndexFieldReference<Double> doubleF;
		final IndexFieldReference<Float> floatF;
		final IndexFieldReference<BigInteger> bigInteger;
		final IndexFieldReference<BigDecimal> bigDecimal;
		final IndexFieldReference<String> style;
		final IndexObjectFieldReference object;
		final IndexFieldReference<Integer> nestedInteger;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().aggregable( Aggregable.YES ) ).toReference();
			converted = root.field( "converted", f -> f.asInteger().aggregable( Aggregable.YES )
					.projectionConverter( String.class, (value, context) -> value.toString() ) ).toReference();
			doubleF = root.field( "doubleF", f -> f.asDouble().aggregable( Aggregable.YES ) ).toReference();
			floatF = root.field( "floatF", f -> f.asFloat().aggregable( Aggregable.YES ) ).toReference();
			bigInteger = root.field( "bigInteger", f -> f.asBigInteger().decimalScale( 0 ).aggregable( Aggregable.YES ) )
					.toReference();
			bigDecimal = root.field( "bigDecimal", f -> f.asBigDecimal().decimalScale( 2 ).aggregable( Aggregable.YES ) )
					.toReference();
			style = root.field( "style", f -> f.asString() ).toReference();

			IndexSchemaObjectField nested = root.objectField( "object", ObjectStructure.NESTED );
			object = nested.toReference();
			nestedInteger = nested.field( "nestedInteger", f -> f.asInteger().aggregable( Aggregable.YES ) )
					.toReference();
		}
	}
}
