/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SearchMultiIndexIT {

	private static final String BACKEND_1 = "backend_1";
	private static final String BACKEND_2 = "backend_2";

	private static final String STRING_1 = "string_1";
	private static final String STRING_2 = "string_2";

	// Backend 1 / Index 1

	private static final String DOCUMENT_1_1_1 = "1_1_1";
	private static final String ADDITIONAL_FIELD_1_1_1 = "additional_field_1_1_1";
	private static final String SORT_FIELD_1_1_1 = "1_1_1";
	private static final String DIFFERENT_TYPES_FIELD_1_1_1 = "different_types_field_1_1_1";

	private static final String DOCUMENT_1_1_2 = "1_1_2";
	private static final String ADDITIONAL_FIELD_1_1_2 = "additional_field_1_1_2";
	private static final String SORT_FIELD_1_1_2 = "1_1_2";
	private static final String DIFFERENT_TYPES_FIELD_1_1_2 = "different_types_field_1_1_2";

	// Backend 1 / Index 2

	private static final String DOCUMENT_1_2_1 = "1_2_1";
	private static final String SORT_FIELD_1_2_1 = "1_2_1";
	private static final Integer DIFFERENT_TYPES_FIELD_1_2_1 = 37;

	// Backend 2 / Index 1

	private static final String DOCUMENT_2_1_1 = "2_1_1";

	private static final String DOCUMENT_2_1_2 = "2_1_2";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding_1_1> index_1_1 =
			SimpleMappedIndex.of( IndexBinding_1_1::new ).backendName( BACKEND_1 ).name( "index_1_1" );
	private final SimpleMappedIndex<IndexBinding_1_2> index_1_2 =
			SimpleMappedIndex.of( IndexBinding_1_2::new ).backendName( BACKEND_1 ).name( "index_1_2" );;
	private final SimpleMappedIndex<IndexBinding_2_1> index_2_1 =
			SimpleMappedIndex.of( IndexBinding_2_1::new ).backendName( BACKEND_2 ).name( "index_2_1" );;

	@BeforeEach
	void setup() {
		setupHelper.start( BACKEND_1 ).withIndexes( index_1_1, index_1_2 ).setup();
		setupHelper.start( BACKEND_2 ).withIndexes( index_2_1 ).setup();

		initData();
	}

	/**
	 * Test that searching on a single index will only target that index.
	 *
	 * Non-regression test for HSEARCH-3977, where the Elasticsearch backend was ignoring the index selection.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3977")
	void search_singleIndex() {
		SearchQuery<DocumentReference> query = index_1_1.query()
				.where( f -> f.match().field( "string" ).matching( STRING_1 ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( c -> {
			c.doc( index_1_1.typeName(), DOCUMENT_1_1_1 );
		} );

		try ( SearchScroll<DocumentReference> scroll = query.scroll( 20 ) ) {
			SearchScrollResult<DocumentReference> chunk = scroll.next();
			assertThatHits( chunk.hits() ).hasDocRefHitsAnyOrder( index_1_1.typeName(), DOCUMENT_1_1_1 );
			assertThat( chunk.total().hitCount() ).isEqualTo( 1L );
		}
		assertThat( query.fetchTotalHitCount() ).isEqualTo( 1L );
	}

	@Test
	void search_across_multiple_indexes() {
		StubMappingScope scope = index_1_1.createScope( index_1_2 );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "string" ).matching( STRING_1 ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( c -> {
			c.doc( index_1_1.typeName(), DOCUMENT_1_1_1 );
			c.doc( index_1_2.typeName(), DOCUMENT_1_2_1 );
		} );
	}

	@Test
	void sort_across_multiple_indexes() {
		StubMappingScope scope = index_1_1.createScope( index_1_2 );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "sortField" ).asc() )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( index_1_1.typeName(), DOCUMENT_1_1_1 );
			c.doc( index_1_1.typeName(), DOCUMENT_1_1_2 );
			c.doc( index_1_2.typeName(), DOCUMENT_1_2_1 );
		} );

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "sortField" ).desc() )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( index_1_2.typeName(), DOCUMENT_1_2_1 );
			c.doc( index_1_1.typeName(), DOCUMENT_1_1_2 );
			c.doc( index_1_1.typeName(), DOCUMENT_1_1_1 );
		} );
	}

	@Test
	void projection_across_multiple_indexes() {
		StubMappingScope scope = index_1_1.createScope( index_1_2 );

		SearchQuery<String> query = scope.query()
				.select( f -> f.field( "sortField", String.class ) )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				SORT_FIELD_1_1_1,
				SORT_FIELD_1_1_2,
				SORT_FIELD_1_2_1
		);
	}

	@Test
	void field_in_one_index_only_is_supported() {
		StubMappingScope scope = index_1_1.createScope( index_1_2 );

		// Predicate
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "additionalField" ).matching( ADDITIONAL_FIELD_1_1_1 ) )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsAnyOrder( index_1_1.typeName(), DOCUMENT_1_1_1 );

		// Sort

		// It doesn't work with Elasticsearch as Elasticsearch is supposed to throw an error if there is no mapping information.
		// In our case, it does not throw an error but simply ignores the results from the second index.
		// See the additional test in the Lucene backend.

		// Projection

		SearchQuery<String> projectionQuery = scope.query()
				.select( f -> f.field( "additionalField", String.class ) )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( projectionQuery ).hasHitsAnyOrder(
				ADDITIONAL_FIELD_1_1_1,
				ADDITIONAL_FIELD_1_1_2,
				null
		);
	}

	@Test
	void unknown_field_throws_exception() {
		StubMappingScope scope = index_1_1.createScope( index_1_2 );

		// Predicate
		assertThatThrownBy(
				() -> scope.predicate().match().field( "unknownField" ),
				"predicate on unknown field with multiple targeted indexes"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field 'unknownField'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames(
								index_1_1.name(),
								index_1_2.name()
						)
				) );

		// Sort

		assertThatThrownBy(
				() -> scope.sort().field( "unknownField" ),
				"sort on unknown field with multiple targeted indexes"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field 'unknownField'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames(
								index_1_1.name(),
								index_1_2.name()
						)
				) );

		// Projection

		assertThatThrownBy(
				() -> scope.projection().field( "unknownField", Object.class ),
				"projection on unknown field with multiple targeted indexes"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "unknownField" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames(
								index_1_1.name(),
								index_1_2.name()
						)
				) );
	}

	@Test
	void search_with_incompatible_types_throws_exception() {
		StubMappingScope scope = index_1_1.createScope( index_1_2 );

		assertThatThrownBy(
				() -> scope.predicate().match().field( "differentTypesField" )
						.matching( DIFFERENT_TYPES_FIELD_1_1_1 ),
				"predicate on field with different type among the targeted indexes"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Inconsistent configuration", "'differentTypesField'" );

		assertThatThrownBy(
				() -> scope.projection().field( "differentTypesField" ).toProjection(),
				"projection on field with different type among the targeted indexes"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Inconsistent configuration", "'differentTypesField'" );

		assertThatThrownBy(
				() -> scope.sort().field( "differentTypesField" ),
				"sort on field with different type among the targeted indexes"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Inconsistent configuration", "'differentTypesField'" );
	}

	@Test
	void search_across_backends_throws_exception() {
		assertThatThrownBy(
				() -> index_1_1.createScope( index_2_1 ),
				"search across multiple backends"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid multi-index scope", "a scope cannot span multiple", "backends" );
	}

	private void initData() {
		BulkIndexer indexer_1_1 = index_1_1.bulkIndexer()
				.add( DOCUMENT_1_1_1, document -> {
					document.addValue( index_1_1.binding().string, STRING_1 );
					document.addValue( index_1_1.binding().additionalField, ADDITIONAL_FIELD_1_1_1 );
					document.addValue( index_1_1.binding().differentTypesField, DIFFERENT_TYPES_FIELD_1_1_1 );
					document.addValue( index_1_1.binding().sortField, SORT_FIELD_1_1_1 );
				} )
				.add( DOCUMENT_1_1_2, document -> {
					document.addValue( index_1_1.binding().string, STRING_2 );
					document.addValue( index_1_1.binding().additionalField, ADDITIONAL_FIELD_1_1_2 );
					document.addValue( index_1_1.binding().differentTypesField, DIFFERENT_TYPES_FIELD_1_1_2 );
					document.addValue( index_1_1.binding().sortField, SORT_FIELD_1_1_2 );
				} );
		BulkIndexer indexer_1_2 = index_1_2.bulkIndexer()
				.add( DOCUMENT_1_2_1, document -> {
					document.addValue( index_1_2.binding().string, STRING_1 );
					document.addValue( index_1_2.binding().differentTypesField, DIFFERENT_TYPES_FIELD_1_2_1 );
					document.addValue( index_1_2.binding().sortField, SORT_FIELD_1_2_1 );
				} );
		BulkIndexer indexer_2_1 = index_2_1.bulkIndexer()
				.add( DOCUMENT_2_1_1, document -> {
					document.addValue( index_2_1.binding().string, STRING_1 );
				} )
				.add( DOCUMENT_2_1_2, document -> {
					document.addValue( index_2_1.binding().string, STRING_2 );
				} );
		indexer_1_1.join( indexer_1_2, indexer_2_1 );
	}

	private static class IndexBinding_1_1 {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> additionalField;
		final IndexFieldReference<String> differentTypesField;
		final IndexFieldReference<String> sortField;

		IndexBinding_1_1(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
			additionalField = root.field(
					"additionalField",
					f -> f.asString().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
			differentTypesField = root.field(
					"differentTypesField",
					f -> f.asString().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
			sortField = root.field(
					"sortField",
					f -> f.asString().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
		}
	}

	private static class IndexBinding_1_2 {
		final IndexFieldReference<String> string;
		final IndexFieldReference<Integer> differentTypesField;
		final IndexFieldReference<String> sortField;

		IndexBinding_1_2(IndexSchemaElement root) {
			string = root.field(
					"string",
					f -> f.asString()
			)
					.toReference();
			differentTypesField = root.field(
					"differentTypesField",
					f -> f.asInteger().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
			sortField = root.field(
					"sortField",
					f -> f.asString().sortable( Sortable.YES ).projectable( Projectable.YES )
			)
					.toReference();
		}
	}

	private static class IndexBinding_2_1 {
		final IndexFieldReference<String> string;

		IndexBinding_2_1(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
		}
	}
}
