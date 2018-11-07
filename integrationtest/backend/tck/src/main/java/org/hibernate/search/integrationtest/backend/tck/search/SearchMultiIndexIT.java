/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SearchMultiIndexIT {

	private static final String BACKEND_1 = "backend_1";
	private static final String BACKEND_2 = "backend_2";

	private static final String STRING_1 = "string_1";
	private static final String STRING_2 = "string_2";

	// Backend 1 / Index 1

	private static final String INDEX_NAME_1_1 = "IndexName_1_1";

	private static final String DOCUMENT_1_1_1 = "1_1_1";
	private static final String ADDITIONAL_FIELD_1_1_1 = "additional_field_1_1_1";
	private static final String SORT_FIELD_1_1_1 = "1_1_1";
	private static final String DIFFERENT_TYPES_FIELD_1_1_1 = "different_types_field_1_1_1";

	private static final String DOCUMENT_1_1_2 = "1_1_2";
	private static final String ADDITIONAL_FIELD_1_1_2 = "additional_field_1_1_2";
	private static final String SORT_FIELD_1_1_2 = "1_1_2";
	private static final String DIFFERENT_TYPES_FIELD_1_1_2 = "different_types_field_1_1_2";

	// Backend 1 / Index 2

	private static final String INDEX_NAME_1_2 = "IndexName_1_2";

	private static final String DOCUMENT_1_2_1 = "1_2_1";
	private static final String SORT_FIELD_1_2_1 = "1_2_1";
	private static final Integer DIFFERENT_TYPES_FIELD_1_2_1 = 37;

	// Backend 2 / Index 1

	private static final String INDEX_NAME_2_1 = "IndexName_2_1";

	private static final String DOCUMENT_2_1_1 = "2_1_1";

	private static final String DOCUMENT_2_1_2 = "2_1_2";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	// Backend 1 / Index 1

	private IndexAccessors_1_1 indexAccessors_1_1;
	private StubMappingIndexManager indexManager_1_1;

	// Backend 1 / Index 2

	private IndexAccessors_1_2 indexAccessors_1_2;
	private StubMappingIndexManager indexManager_1_2;

	// Backend 2 / Index 1

	private IndexAccessors_2_1 indexAccessors_2_1;
	private StubMappingIndexManager indexManager_2_1;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration( BACKEND_1 )
				.withIndex(
						"MappedType_1_1", INDEX_NAME_1_1,
						ctx -> this.indexAccessors_1_1 = new IndexAccessors_1_1( ctx.getSchemaElement() ),
						indexMapping -> this.indexManager_1_1 = indexMapping
				)
				.withIndex(
						"MappedType_1_2", INDEX_NAME_1_2,
						ctx -> this.indexAccessors_1_2 = new IndexAccessors_1_2( ctx.getSchemaElement() ),
						indexMapping -> this.indexManager_1_2 = indexMapping
				)
				.setup();

		setupHelper.withDefaultConfiguration( BACKEND_2 )
				.withIndex(
						"MappedType_2_1", INDEX_NAME_2_1,
						ctx -> this.indexAccessors_2_1 = new IndexAccessors_2_1( ctx.getSchemaElement() ),
						indexMapping -> this.indexManager_2_1 = indexMapping
				)
				.setup();

		initData();
	}

	@Test
	public void search_across_multiple_indexes() {
		StubMappingSearchTarget searchTarget = indexManager_1_1.createSearchTarget( indexManager_1_2 );

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.match().onField( "string" ).matching( STRING_1 ).toPredicate() )
				.build();

		assertThat( query ).hasDocRefHitsAnyOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_1 );
			c.doc( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
		} );
	}

	@Test
	public void sort_across_multiple_indexes() {
		StubMappingSearchTarget searchTarget = indexManager_1_1.createSearchTarget( indexManager_1_2 );

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.sort( c -> c.byField( "sortField" ).asc() )
				.build();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_1 );
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2 );
			c.doc( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
		} );

		query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.sort( c -> c.byField( "sortField" ).desc() )
				.build();

		assertThat( query ).hasDocRefHitsExactOrder( c -> {
			c.doc( INDEX_NAME_1_2, DOCUMENT_1_2_1 );
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_2 );
			c.doc( INDEX_NAME_1_1, DOCUMENT_1_1_1 );
		} );
	}

	@Test
	public void projection_across_multiple_indexes() {
		StubMappingSearchTarget searchTarget = indexManager_1_1.createSearchTarget( indexManager_1_2 );

		SearchQuery<List<?>> query = searchTarget.query()
				.asProjections( searchTarget.projection().field( "sortField", String.class ).toProjection() )
				.predicate( f -> f.matchAll().toPredicate() )
				.build();

		assertThat( query ).hasListHitsAnyOrder( c -> {
			c.list( SORT_FIELD_1_1_1 );
			c.list( SORT_FIELD_1_1_2 );
			c.list( SORT_FIELD_1_2_1 );
		} );
	}

	@Test
	public void field_in_one_index_only_is_supported() {
		StubMappingSearchTarget searchTarget = indexManager_1_1.createSearchTarget( indexManager_1_2 );

		// Predicate
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.match().onField( "additionalField" ).matching( ADDITIONAL_FIELD_1_1_1 ).toPredicate() )
				.build();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_1_1, DOCUMENT_1_1_1 );

		// Sort

		// It doesn't work with Elasticsearch as Elasticsearch is supposed to throw an error if there is no mapping information.
		// In our case, it does not throw an error but simply ignores the results from the second index.
		// See the additional test in the Lucene backend.

		// Projection

		SearchQuery<List<?>> projectionQuery = searchTarget.query()
				.asProjections( searchTarget.projection().field( "additionalField", String.class ).toProjection() )
				.predicate( f -> f.matchAll().toPredicate() )
				.build();

		assertThat( projectionQuery ).hasListHitsAnyOrder( c -> {
			c.list( ADDITIONAL_FIELD_1_1_1 );
			c.list( ADDITIONAL_FIELD_1_1_2 );
			c.list( null );
		} );
	}

	@Test
	public void unknown_field_throws_exception() {
		StubMappingSearchTarget searchTarget = indexManager_1_1.createSearchTarget( indexManager_1_2 );

		// Predicate
		SubTest.expectException(
				"predicate on unknown field with multiple targeted indexes",
				() -> searchTarget.predicate().match().onField( "unknownField" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field 'unknownField'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames(
								INDEX_NAME_1_1,
								INDEX_NAME_1_2
						)
				) );

		// Sort

		SubTest.expectException(
				"sort on unknown field with multiple targeted indexes",
				() -> searchTarget.sort().byField( "unknownField" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field 'unknownField'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames(
								INDEX_NAME_1_1,
								INDEX_NAME_1_2
						)
				) );

		// Projection

		SubTest.expectException(
				"projection on unknown field with multiple targeted indexes",
				() -> searchTarget.query().asProjections(
						searchTarget.projection().field( "unknownField", Object.class ).toProjection()
				)
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "unknownField" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames(
								INDEX_NAME_1_1,
								INDEX_NAME_1_2
						)
				) );
	}

	@Test
	public void search_with_incompatible_types_throws_exception() {
		StubMappingSearchTarget searchTarget = indexManager_1_1.createSearchTarget( indexManager_1_2 );

		SubTest.expectException(
				"predicate on field with different type among the targeted indexes",
				() -> searchTarget.predicate().match().onField( "differentTypesField" )
						.matching( DIFFERENT_TYPES_FIELD_1_1_1 )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate for field 'differentTypesField'" );

		SubTest.expectException(
				"projection on field with different type among the targeted indexes",
				() -> searchTarget.projection().field( "differentTypesField" ).toProjection()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a projection for field 'differentTypesField'" );

		SubTest.expectException(
				"sort on field with different type among the targeted indexes",
				() -> searchTarget.sort().byField( "differentTypesField" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a sort for field 'differentTypesField'" );
	}

	@Test
	public void search_across_backends_throws_exception() {
		SubTest.expectException(
				"search across multiple backends",
				() -> indexManager_1_1.createSearchTarget( indexManager_2_1 )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "A search query cannot target multiple" )
				.hasMessageContaining( "backends" );
	}

	private void initData() {
		// Backend 1 / Index 1

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager_1_1.createWorkPlan();

		workPlan.add( referenceProvider( DOCUMENT_1_1_1 ), document -> {
			indexAccessors_1_1.string.write( document, STRING_1 );
			indexAccessors_1_1.additionalField.write( document, ADDITIONAL_FIELD_1_1_1 );
			indexAccessors_1_1.differentTypesField.write( document, DIFFERENT_TYPES_FIELD_1_1_1 );
			indexAccessors_1_1.sortField.write( document, SORT_FIELD_1_1_1 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_1_1_2 ), document -> {
			indexAccessors_1_1.string.write( document, STRING_2 );
			indexAccessors_1_1.additionalField.write( document, ADDITIONAL_FIELD_1_1_2 );
			indexAccessors_1_1.differentTypesField.write( document, DIFFERENT_TYPES_FIELD_1_1_2 );
			indexAccessors_1_1.sortField.write( document, SORT_FIELD_1_1_2 );
		} );

		workPlan.execute().join();

		StubMappingSearchTarget searchTarget = indexManager_1_1.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_1_1, DOCUMENT_1_1_1, DOCUMENT_1_1_2 );

		// Backend 1 / Index 2

		workPlan = indexManager_1_2.createWorkPlan();

		workPlan.add( referenceProvider( DOCUMENT_1_2_1 ), document -> {
			indexAccessors_1_2.string.write( document, STRING_1 );
			indexAccessors_1_2.differentTypesField.write( document, DIFFERENT_TYPES_FIELD_1_2_1 );
			indexAccessors_1_2.sortField.write( document, SORT_FIELD_1_2_1 );
		} );

		workPlan.execute().join();

		searchTarget = indexManager_1_2.createSearchTarget();
		query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_1_2, DOCUMENT_1_2_1 );

		// Backend 2 / Index 1

		workPlan = indexManager_2_1.createWorkPlan();

		workPlan.add( referenceProvider( DOCUMENT_2_1_1 ), document -> {
			indexAccessors_2_1.string.write( document, STRING_1 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2_1_2 ), document -> {
			indexAccessors_2_1.string.write( document, STRING_2 );
		} );

		workPlan.execute().join();

		searchTarget = indexManager_2_1.createSearchTarget();
		query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME_2_1, DOCUMENT_2_1_1, DOCUMENT_2_1_2 );
	}

	private static class IndexAccessors_1_1 {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<String> additionalField;
		final IndexFieldAccessor<String> differentTypesField;
		final IndexFieldAccessor<String> sortField;

		IndexAccessors_1_1(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
			additionalField = root.field( "additionalField" ).asString().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
			differentTypesField = root.field( "differentTypesField" ).asString().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
			sortField = root.field( "sortField" ).asString().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
		}
	}

	private static class IndexAccessors_1_2 {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<Integer> differentTypesField;
		final IndexFieldAccessor<String> sortField;

		IndexAccessors_1_2(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
			differentTypesField = root.field( "differentTypesField" ).asInteger().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
			sortField = root.field( "sortField" ).asString().sortable( Sortable.YES ).projectable( Projectable.YES ).createAccessor();
		}
	}

	private static class IndexAccessors_2_1 {
		final IndexFieldAccessor<String> string;

		IndexAccessors_2_1(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
		}
	}
}
