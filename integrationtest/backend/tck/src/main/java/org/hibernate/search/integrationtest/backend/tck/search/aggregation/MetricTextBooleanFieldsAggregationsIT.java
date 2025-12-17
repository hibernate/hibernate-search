/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

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

class MetricTextBooleanFieldsAggregationsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );

	private final AggregationKey<Long> countTexts = AggregationKey.of( "countTexts" );
	private final AggregationKey<Long> countDistinctTexts = AggregationKey.of( "countDistinctTexts" );
	private final AggregationKey<Long> countValuesTextMultiValued = AggregationKey.of( "countValuesTextMultiValued" );
	private final AggregationKey<Long> countDistinctValuesTextMultiValued =
			AggregationKey.of( "countDistinctValuesTextMultiValued" );
	private final AggregationKey<Long> countValuesNestedTextMultiValued =
			AggregationKey.of( "countValuesNestedTextMultiValued" );
	private final AggregationKey<Long> countDistinctValuesNestedTextMultiValued =
			AggregationKey.of( "countDistinctValuesNestedTextMultiValued" );

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
		assertThat( result.aggregation( countTexts ) ).isEqualTo( 5 );
		assertThat( result.aggregation( countDistinctTexts ) ).isEqualTo( 3 );
		assertThat( result.aggregation( countValuesTextMultiValued ) ).isEqualTo( 25 );
		assertThat( result.aggregation( countDistinctValuesTextMultiValued ) ).isEqualTo( 15 );
		assertThat( result.aggregation( countValuesNestedTextMultiValued ) ).isEqualTo( 25L );
		assertThat( result.aggregation( countDistinctValuesNestedTextMultiValued ) ).isEqualTo( 15L );
	}

	@Test
	void test_allResults() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQueryOptionsStep<?, ?, DocumentReference, StubLoadingOptionsStep, ?, ?> options = scope.query()
				.where( f -> f.matchAll() );
		SearchQuery<DocumentReference> query = defineAggregations( options );

		SearchResult<DocumentReference> result = query.fetch( 0 );
		assertThat( result.aggregation( countTexts ) ).isEqualTo( 10 );
		assertThat( result.aggregation( countDistinctTexts ) ).isEqualTo( 6 );
		assertThat( result.aggregation( countValuesTextMultiValued ) ).isEqualTo( 50 );
		assertThat( result.aggregation( countDistinctValuesTextMultiValued ) ).isEqualTo( 30 );
		assertThat( result.aggregation( countValuesNestedTextMultiValued ) ).isEqualTo( 50L );
		assertThat( result.aggregation( countDistinctValuesNestedTextMultiValued ) ).isEqualTo( 30L );
	}

	private SearchQuery<DocumentReference> defineAggregations(
			SearchQueryOptionsStep<?, ?, DocumentReference, StubLoadingOptionsStep, ?, ?> options) {

		return options
				.aggregation( countTexts, f -> f.count().field( "text" ) )
				.aggregation( countDistinctTexts, f -> f.count().field( "text" ).distinct() )
				.aggregation( countDistinctValuesTextMultiValued, f -> f.count().field( "textMultiValued" ).distinct() )
				.aggregation( countValuesTextMultiValued, f -> f.count().field( "textMultiValued" ) )
				.aggregation( countValuesNestedTextMultiValued, f -> f.count().field( "object.nestedTextMultiValued" ) )
				.aggregation( countDistinctValuesNestedTextMultiValued,
						f -> f.count().field( "object.nestedTextMultiValued" ).distinct() )
				.toQuery();
	}

	@SuppressWarnings("unused") // For EJC and lambda arg
	private void initData() {
		Integer[] integers = new Integer[] { 9, 18, 3, 18, 7, -10, 3, 0, 7, 0, null };
		String[] styles = new String[] { "bla", "aaa" };

		BulkIndexer bulkIndexer = mainIndex.bulkIndexer();
		for ( int i = 0; i < integers.length; i++ ) {
			String value = integers[i] == null ? null : "text " + integers[i].toString();
			String style = styles[i % 2];
			String id = i + ":" + value + ":" + style;
			boolean bool = i % 2 == 0;

			bulkIndexer.add( id, document -> {
				document.addValue( mainIndex.binding().text, value );
				document.addValue( mainIndex.binding().bool, bool );
				document.addValue( mainIndex.binding().style, style );

				for ( int j = 0; j < 5; j++ ) {
					String v = value == null ? null : value + "-" + j;
					document.addValue( mainIndex.binding().textMultiValued, v );
				}

				DocumentElement object = document.addObject( mainIndex.binding().object );
				object.addValue( mainIndex.binding().nestedText, value );
				for ( int j = 0; j < 5; j++ ) {
					String v = value == null ? null : value + "-" + j;
					object.addValue( mainIndex.binding().nestedTextMultiValued, v );
				}
			} ).join();
		}
		bulkIndexer.add( "empty", document -> {} )
				.join();
	}

	@SuppressWarnings("unused")
	private static class IndexBinding {
		final IndexFieldReference<String> text;
		final IndexFieldReference<String> textMultiValued;
		final IndexFieldReference<Boolean> bool;
		final IndexFieldReference<String> style;
		final IndexObjectFieldReference object;
		final IndexFieldReference<String> nestedText;
		final IndexFieldReference<String> nestedTextMultiValued;

		IndexBinding(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString().aggregable( Aggregable.YES ) ).toReference();
			textMultiValued =
					root.field( "textMultiValued", f -> f.asString().aggregable( Aggregable.YES ) ).multiValued().toReference();
			bool = root.field( "bool", f -> f.asBoolean().aggregable( Aggregable.YES ) ).toReference();
			style = root.field( "style", f -> f.asString() ).toReference();

			IndexSchemaObjectField nested = root.objectField( "object", ObjectStructure.NESTED );
			object = nested.toReference();
			nestedText = nested.field( "nestedText", f -> f.asString().aggregable( Aggregable.YES ) ).toReference();
			nestedTextMultiValued = nested.field( "nestedTextMultiValued", f -> f.asString().aggregable( Aggregable.YES ) )
					.multiValued().toReference();
		}
	}
}
