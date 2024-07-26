/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

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

class MetricTemporalFieldsAggregationsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final AggregationKey<LocalDate> sumDates = AggregationKey.of( "sumDates" );
	private final AggregationKey<String> sumConverted = AggregationKey.of( "sumConverted" );
	private final AggregationKey<LocalDate> sumConvertedNoConversion = AggregationKey.of( "sumConvertedNoConversion" );
	private final AggregationKey<LocalDate> sumFiltered = AggregationKey.of( "sumFiltered" );
	private final AggregationKey<LocalDate> minDates = AggregationKey.of( "minDates" );
	private final AggregationKey<String> minConverted = AggregationKey.of( "minConverted" );
	private final AggregationKey<LocalDate> maxDates = AggregationKey.of( "maxDates" );
	private final AggregationKey<String> maxConverted = AggregationKey.of( "maxConverted" );
	private final AggregationKey<Long> countDates = AggregationKey.of( "countDates" );
	private final AggregationKey<Long> countConverted = AggregationKey.of( "countConverted" );
	private final AggregationKey<Long> countDistinctDates = AggregationKey.of( "countDistinctDates" );
	private final AggregationKey<Long> countDistinctConverted = AggregationKey.of( "countDistinctConverted" );
	private final AggregationKey<LocalDate> avgDates = AggregationKey.of( "avgDates" );
	private final AggregationKey<String> avgConverted = AggregationKey.of( "avgConverted" );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndexes( mainIndex ).setup().integration();
		initData();
	}

	@Test
	void test_filteringResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQueryOptionsStep<?, DocumentReference, StubLoadingOptionsStep, ?, ?> options = scope.query()
				.where( f -> f.match().field( "style" ).matching( "bla" ) );
		SearchQuery<DocumentReference> query = defineAggregations( options );

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( sumDates ) ).isEqualTo( LocalDate.of( 2204, Month.SEPTEMBER, 16 ) );
		assertThat( result.aggregation( sumConverted ) ).isEqualTo( "2204-09-16" );
		assertThat( result.aggregation( sumConvertedNoConversion ) ).isEqualTo( LocalDate.of( 2204, Month.SEPTEMBER, 16 ) );
		assertThat( result.aggregation( sumFiltered ) ).isEqualTo( LocalDate.of( 2063, Month.NOVEMBER, 25 ) );
		assertThat( result.aggregation( minDates ) ).isEqualTo( LocalDate.of( 2016, Month.DECEMBER, 6 ) );
		assertThat( result.aggregation( minConverted ) ).isEqualTo( "2016-12-06" );
		assertThat( result.aggregation( maxDates ) ).isEqualTo( LocalDate.of( 2016, Month.DECEMBER, 14 ) );
		assertThat( result.aggregation( maxConverted ) ).isEqualTo( "2016-12-14" );
		assertThat( result.aggregation( countDates ) ).isEqualTo( 5 );
		assertThat( result.aggregation( countConverted ) ).isEqualTo( 5 );
		assertThat( result.aggregation( countDistinctDates ) ).isEqualTo( 5 );
		assertThat( result.aggregation( countDistinctConverted ) ).isEqualTo( 5 );
		assertThat( result.aggregation( avgDates ) ).isEqualTo( LocalDate.of( 2016, Month.DECEMBER, 10 ) );
		assertThat( result.aggregation( avgConverted ) ).isEqualTo( "2016-12-10" );
	}

	@Test
	void test_allResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQueryOptionsStep<?, DocumentReference, StubLoadingOptionsStep, ?, ?> options = scope.query()
				.where( f -> f.matchAll() );
		SearchQuery<DocumentReference> query = defineAggregations( options );

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( sumDates ) ).isEqualTo( LocalDate.of( 2439, Month.JUNE, 6 ) );
		assertThat( result.aggregation( sumConverted ) ).isEqualTo( "2439-06-06" );
		assertThat( result.aggregation( sumConvertedNoConversion ) ).isEqualTo( LocalDate.of( 2439, Month.JUNE, 6 ) );
		assertThat( result.aggregation( sumFiltered ) ).isEqualTo( LocalDate.of( 2204, Month.OCTOBER, 1 ) );
		assertThat( result.aggregation( minDates ) ).isEqualTo( LocalDate.of( 2016, Month.DECEMBER, 6 ) );
		assertThat( result.aggregation( minConverted ) ).isEqualTo( "2016-12-06" );
		assertThat( result.aggregation( maxDates ) ).isEqualTo( LocalDate.of( 2016, Month.DECEMBER, 15 ) );
		assertThat( result.aggregation( maxConverted ) ).isEqualTo( "2016-12-15" );
		assertThat( result.aggregation( countDates ) ).isEqualTo( 10 );
		assertThat( result.aggregation( countConverted ) ).isEqualTo( 10 );
		assertThat( result.aggregation( countDistinctDates ) ).isEqualTo( 10 );
		assertThat( result.aggregation( countDistinctConverted ) ).isEqualTo( 10 );
		assertThat( result.aggregation( avgDates ) ).isEqualTo( LocalDate.of( 2016, Month.DECEMBER, 10 ) );
		assertThat( result.aggregation( avgConverted ) ).isEqualTo( "2016-12-10" );
	}

	private SearchQuery<DocumentReference> defineAggregations(
			SearchQueryOptionsStep<?, DocumentReference, StubLoadingOptionsStep, ?, ?> options) {
		return options
				.aggregation( sumDates, f -> f.sum().field( "date", LocalDate.class ) )
				.aggregation( sumConverted, f -> f.sum().field( "converted", String.class ) )
				.aggregation(
						sumConvertedNoConversion, f -> f.sum().field( "converted", LocalDate.class, ValueModel.INDEX ) )
				.aggregation( sumFiltered, f -> f.sum().field( "object.nestedDate", LocalDate.class )
						.filter( ff -> ff.range().field( "object.nestedDate" )
								.atLeast( LocalDate.of( 2016, Month.DECEMBER, 11 ) ) ) )
				.aggregation( minDates, f -> f.min().field( "date", LocalDate.class ) )
				.aggregation( minConverted, f -> f.min().field( "converted", String.class ) )
				.aggregation( maxDates, f -> f.max().field( "date", LocalDate.class ) )
				.aggregation( maxConverted, f -> f.max().field( "converted", String.class ) )
				.aggregation( countDates, f -> f.count().field( "date" ) )
				.aggregation( countConverted, f -> f.count().field( "converted" ) )
				.aggregation( countDistinctDates, f -> f.countDistinct().field( "date" ) )
				.aggregation( countDistinctConverted, f -> f.countDistinct().field( "converted" ) )
				.aggregation( avgDates, f -> f.avg().field( "date", LocalDate.class ) )
				.aggregation( avgConverted, f -> f.avg().field( "converted", String.class ) )
				.toQuery();
	}

	private void initData() {
		LocalDate baseDate = LocalDate.of( 2016, Month.DECEMBER, 6 );
		int[] integers = new int[] { 9, 18, 3, 18, 7, -10, 3, 0, 7, 0 };
		String[] styles = new String[] { "bla", "aaa" };

		BulkIndexer bulkIndexer = mainIndex.bulkIndexer();
		for ( int i = 0; i < integers.length; i++ ) {
			int value = integers[i];
			String style = styles[i % 2];
			String id = i + ":" + value + ":" + style;
			LocalDate date = baseDate.plusDays( i );

			bulkIndexer.add( id, document -> {
				document.addValue( mainIndex.binding().date, date );
				document.addValue( mainIndex.binding().converted, date );
				document.addValue( mainIndex.binding().style, style );

				DocumentElement object = document.addObject( mainIndex.binding().object );
				object.addValue( mainIndex.binding().nestedDate, date );
			} );
		}
		bulkIndexer.add( "empty", document -> {} ).join();
	}

	@SuppressWarnings("unused")
	private static class IndexBinding {
		final IndexFieldReference<LocalDate> date;
		final IndexFieldReference<LocalDate> converted;
		final IndexFieldReference<String> style;
		final IndexObjectFieldReference object;
		final IndexFieldReference<LocalDate> nestedDate;

		IndexBinding(IndexSchemaElement root) {
			date = root.field( "date", f -> f.asLocalDate().aggregable( Aggregable.YES ) ).toReference();
			converted = root.field( "converted", f -> f.asLocalDate().aggregable( Aggregable.YES )
					.projectionConverter( String.class, (value, context) -> value.toString() ) ).toReference();
			style = root.field( "style", f -> f.asString() ).toReference();

			IndexSchemaObjectField nested = root.objectField( "object", ObjectStructure.NESTED );
			object = nested.toReference();
			nestedDate = nested.field( "nestedDate", f -> f.asLocalDate().aggregable( Aggregable.YES ) )
					.toReference();
		}
	}
}
