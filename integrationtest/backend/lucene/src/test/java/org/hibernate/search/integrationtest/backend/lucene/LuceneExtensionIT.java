/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.assertThatDocument;
import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.projection.dsl.DocumentTree;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchScroll;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchScrollResult;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryOptionsStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQuerySelectStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryWhereStep;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

class LuceneExtensionIT {

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String FOURTH_ID = "4";
	private static final String FIFTH_ID = "5";

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<IndexBinding> otherIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "other" );
	private final SimpleMappedIndex<IndexBinding> treeIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "tree" );
	private final SimpleMappedIndex<SomeOtherIndexBinding> someOtherIndex =
			SimpleMappedIndex.of( SomeOtherIndexBinding::new ).name( "someOther" );

	private SearchIntegration integration;

	@BeforeEach
	void setup() {
		this.integration =
				setupHelper.start().withIndexes( mainIndex, otherIndex, treeIndex, someOtherIndex ).setup().integration();

		initData();
	}

	@Test
	@SuppressWarnings("unused")
	void queryContext() {
		StubMappingScope scope = mainIndex.createScope();

		// Put intermediary contexts into variables to check they have the right type
		LuceneSearchQuerySelectStep<EntityReference, DocumentReference, StubLoadingOptionsStep> context1 =
				scope.query().extension( LuceneExtension.get() );
		LuceneSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> context2 = context1.select(
				f -> f.composite()
						.from( f.documentReference(), f.document() )
						// We don't care about the document, it's just to test that the factory context allows Lucene-specific projection
						.as( (docRef, document) -> docRef )
		);
		// Note we can use Lucene-specific predicates immediately
		LuceneSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> context3 =
				context2.where( f -> f.fromLuceneQuery( new MatchAllDocsQuery() ) );
		// Note we can use Lucene-specific sorts immediately
		LuceneSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> context4 =
				context3.sort( f -> f.fromLuceneSortField( new SortedSetSortField( "sort1", false ) ) );

		// Put the query and result into variables to check they have the right type
		LuceneSearchQuery<DocumentReference> query = context4.toQuery();
		LuceneSearchResult<DocumentReference> result = query.fetchAll();

		assertThatResult( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID )
				.hasTotalHitCount( 5 );

		// Also check (at compile time) the context type for other asXXX() methods, since we need to override each method explicitly
		LuceneSearchQueryWhereStep<EntityReference, StubLoadingOptionsStep> selectEntityReferenceContext =
				scope.query().extension( LuceneExtension.get() ).selectEntityReference();
		LuceneSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> selectEntityContext =
				scope.query().extension( LuceneExtension.get() ).selectEntity();
		SearchProjection<DocumentReference> projection = scope.projection().documentReference().toProjection();
		LuceneSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> selectProjectionContext =
				scope.query().extension( LuceneExtension.get() ).select( projection );
		LuceneSearchQueryWhereStep<List<?>, ?> selectProjectionsContext =
				scope.query().extension( LuceneExtension.get() ).select( projection, projection );
		LuceneSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> defaultResultContext =
				scope.query().extension( LuceneExtension.get() )
						.where( f -> f.fromLuceneQuery( new MatchAllDocsQuery() ) );
	}

	@Test
	void query() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> genericQuery = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Put the query and result into variables to check they have the right type
		LuceneSearchQuery<DocumentReference> query = genericQuery.extension( LuceneExtension.get() );
		LuceneSearchResult<DocumentReference> result = query.fetchAll();
		assertThatResult( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID )
				.hasTotalHitCount( 5 );

		// Unsupported extension
		assertThatThrownBy(
				() -> query.extension(
						(SearchQuery<DocumentReference> original, SearchLoadingContext<?> loadingContext) -> Optional.empty() )
		)
				.isInstanceOf( SearchException.class );
	}

	@Test
	void query_topDocs() {
		StubMappingScope scope = mainIndex.createScope();

		LuceneSearchResult<DocumentReference> result = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.matchAll() )
				.fetchAll();

		assertThat( result.topDocs() ).isNotNull();
	}

	@Test
	void query_explain_singleIndex() {
		StubMappingScope scope = mainIndex.createScope();

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		assertThat( query.explain( FIRST_ID ) )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );

		// Non-matching document
		assertThat( query.explain( FIFTH_ID ) )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );
	}

	@Test
	void query_explain_singleIndex_invalidId() {
		StubMappingScope scope = mainIndex.createScope();

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Non-existing document
		assertThatThrownBy(
				() -> query.explain( "InvalidId" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid document identifier: 'InvalidId'",
						"No such document for type '" + mainIndex.typeName() + "'" );
	}

	@Test
	void query_explain_multipleIndexes() {
		StubMappingScope scope = mainIndex.createScope( otherIndex );

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		assertThat( query.explain( mainIndex.typeName(), FIRST_ID ) )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );

		// Non-matching document
		assertThat( query.explain( mainIndex.typeName(), FIFTH_ID ) )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );
	}

	@Test
	void query_explain_multipleIndexes_missingTypeName() {
		StubMappingScope scope = mainIndex.createScope( otherIndex );

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		assertThatThrownBy(
				() -> query.explain( FIRST_ID )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid use of explain(Object id) on a query targeting multiple types",
						"pass one of [" + mainIndex.typeName() + ", " + otherIndex.typeName() + "]" );
	}

	@Test
	void query_explain_multipleIndexes_invalidIndexName() {
		StubMappingScope scope = mainIndex.createScope( otherIndex );

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		assertThatThrownBy(
				() -> query.explain( "NotAMappedName", FIRST_ID )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid mapped type name: 'NotAMappedName'",
						"This type is not among the mapped types targeted by this query: ["
								+ mainIndex.typeName() + ", " + otherIndex.typeName() + "]" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3974")
	void scroll_onFetchable() {
		// Check the scroll has the extended type and works correctly
		try ( LuceneSearchScroll<DocumentReference> scroll = mainIndex.query()
				.extension( LuceneExtension.get() ) // Call extension() on the DSL step
				.where( f -> f.matchAll() )
				.scroll( 20 ) ) { // Call scroll() on the fetchable
			List<DocumentReference> hits = new ArrayList<>();
			// Check the scroll result has the extended type and works correctly
			for ( LuceneSearchScrollResult<DocumentReference> chunk = scroll.next(); chunk.hasHits();
					chunk = scroll.next() ) {
				hits.addAll( chunk.hits() );
				assertThat( chunk.total().hitCount() ).isEqualTo( 5 );
			}
			assertThatHits( hits )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3974")
	void scroll_onQuery() {
		// Check the scroll has the extended type and works correctly
		try ( LuceneSearchScroll<DocumentReference> scroll = mainIndex.query()
				.where( f -> f.matchAll() )
				.toQuery()
				.extension( LuceneExtension.get() ) // Call extension() on the query
				.scroll( 20 ) ) { // Call scroll() on the query
			List<DocumentReference> hits = new ArrayList<>();
			// Check the scroll result has the extended type and works correctly
			for ( LuceneSearchScrollResult<DocumentReference> chunk = scroll.next(); chunk.hasHits();
					chunk = scroll.next() ) {
				hits.addAll( chunk.hits() );
				assertThat( chunk.total().hitCount() ).isEqualTo( 5 );
			}
			assertThatHits( hits )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID );
		}
	}

	@Test
	void predicate_fromLuceneQuery() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.or(
						f.extension( LuceneExtension.get() )
								.fromLuceneQuery( new TermQuery( new Term( "string", "text 1" ) ) ),
						f.extension( LuceneExtension.get() )
								.fromLuceneQuery( IntPoint.newExactQuery( "integer", 2 ) ),
						f.extension( LuceneExtension.get() )
								.fromLuceneQuery( LatLonPoint.newDistanceQuery( "geoPoint", 40, -70, 200_000 ) )
				) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID )
				.hasTotalHitCount( 3 );
	}

	@Test
	void predicate_fromLuceneQuery_separatePredicate() {
		StubMappingScope scope = mainIndex.createScope();

		SearchPredicate predicate1 = scope.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( new TermQuery( new Term( "string", "text 1" ) ) ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( IntPoint.newExactQuery( "integer", 2 ) ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( LatLonPoint.newDistanceQuery( "geoPoint", 40, -70, 200_000 ) ).toPredicate();
		SearchPredicate booleanPredicate = scope.predicate().or(
				predicate1,
				predicate2,
				predicate3
		).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where( booleanPredicate )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID )
				.hasTotalHitCount( 3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	void predicate_fromLuceneQuery_withRoot() {
		SearchQuery<DocumentReference> query = mainIndex.query()
				.where( f -> {
					LuceneSearchPredicateFactory f2 = f.extension( LuceneExtension.get() ).withRoot( "flattenedObject" );
					return f2.or(
							f2.fromLuceneQuery( new TermQuery( new Term( f2.toAbsolutePath( "stringInObject" ), "text 2" ) ) ),
							f2.fromLuceneQuery( IntPoint.newExactQuery( f2.toAbsolutePath( "integerInObject" ), 3 ) ) );
				} )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID )
				.hasTotalHitCount( 2 );
	}

	@Test
	void sort_fromLuceneSortField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f
						.extension( LuceneExtension.get() )
						.fromLuceneSortField( new SortedSetSortField( "sort1", false ) )
						.then().extension( LuceneExtension.get() )
						.fromLuceneSortField( new SortedSetSortField( "sort2", false ) )
						.then().extension( LuceneExtension.get() )
						.fromLuceneSortField( new SortedSetSortField( "sort3", false ) )
				)
				.toQuery();
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
		);

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f
						.extension().ifSupported(
								LuceneExtension.get(),
								c2 -> c2.fromLuceneSort( new Sort(
										new SortedSetSortField( "sort3", false ),
										new SortedSetSortField( "sort2", false ),
										new SortedSetSortField( "sort1", false )
								)
								)
						)
						.orElseFail()
				)
				.toQuery();
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				THIRD_ID, SECOND_ID, FIRST_ID, FOURTH_ID, FIFTH_ID
		);
	}

	@Test
	void sort_fromLuceneSortField_separateSort() {
		StubMappingScope scope = mainIndex.createScope();

		SearchSort sort1 = scope.sort().extension()
				.ifSupported(
						LuceneExtension.get(),
						c2 -> c2.fromLuceneSortField( new SortedSetSortField( "sort1", false ) )
				)
				.orElseFail()
				.toSort();
		SearchSort sort2 = scope.sort().extension( LuceneExtension.get() )
				.fromLuceneSortField( new SortedSetSortField( "sort2", false ) )
				.toSort();
		SearchSort sort3 = scope.sort().extension()
				.ifSupported(
						LuceneExtension.get(),
						c2 -> c2.fromLuceneSortField( new SortedSetSortField( "sort3", false ) )
				)
				.orElseFail()
				.toSort();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.composite().add( sort1 ).add( sort2 ).add( sort3 ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID );

		SearchSort sort = scope.sort()
				.extension( LuceneExtension.get() ).fromLuceneSort( new Sort(
						new SortedSetSortField( "sort3", false ),
						new SortedSetSortField( "sort2", false ),
						new SortedSetSortField( "sort1", false )
				)
				)
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, FOURTH_ID, FIFTH_ID );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	void sort_fromLuceneSortField_withRoot() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.matchAll() )
				.sort( f -> {
					LuceneSearchSortFactory f2 = f.extension( LuceneExtension.get() ).withRoot( "flattenedObject" );
					return f2.fromLuceneSortField( new SortedSetSortField( f2.toAbsolutePath( "sortInObject" ), false ) );
				} ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(),
						FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID );

		assertThatQuery( mainIndex.query()
				.where( f -> f.matchAll() )
				.sort( f -> {
					LuceneSearchSortFactory f2 = f.extension( LuceneExtension.get() ).withRoot( "flattenedObject" );
					return f2.fromLuceneSortField( new SortedSetSortField( f2.toAbsolutePath( "sortInObject" ), true ) );
				} ) )
				.hasDocRefHitsExactOrder( mainIndex.typeName(),
						FIFTH_ID, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID );
	}

	@Test
	void sort_filter_fromLuceneQuery() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.extension( LuceneExtension.get() )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( mainIndex.binding().nestedObject.relativeFieldName + ".sort1" )
						// The provided predicate factory should already be extended and offer Lucene-specific extensions
						.filter( pf -> pf.fromLuceneQuery( new TermQuery( new Term(
								mainIndex.binding().nestedObject.relativeFieldName + ".discriminator",
								"included"
						) ) ) )
				)
				.toQuery();
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
		);

		// Check descending order, just in case the above order was reached by chance.
		query = scope.query()
				.extension( LuceneExtension.get() )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( mainIndex.binding().nestedObject.relativeFieldName + ".sort1" )
						.desc()
						.filter( pf -> pf.fromLuceneQuery( new TermQuery( new Term(
								mainIndex.binding().nestedObject.relativeFieldName + ".discriminator",
								"included"
						) ) ) )
				)
				.toQuery();
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				FIFTH_ID, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID
		);
	}

	@Test
	void predicate_nativeField() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = "nativeField";

		assertThatThrownBy(
				() -> scope.query()
						.where( f -> f.match().field( fieldPath ).matching( "37" ) )
						.toQuery(),
				"match() predicate on unsupported native field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'predicate:match' on field '" + fieldPath + "'",
						"'predicate:match' is not available for fields of this type"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField" )
				) );
	}

	@Test
	void predicate_nativeField_fromLuceneQuery() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.extension( LuceneExtension.get() )
						.fromLuceneQuery( new TermQuery( new Term( "nativeField", "37" ) ) )
				)
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID );
	}

	@Test
	void predicate_nativeField_exists() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = "nativeField";

		assertThatThrownBy(
				() -> scope.predicate().exists().field( fieldPath ),
				"exists() predicate on unsupported native field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'predicate:exists' on field '" + fieldPath + "'",
						"'predicate:exists' is not available for fields of this type"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField" )
				) );
	}

	@Test
	void sort_nativeField() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = "nativeField";

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( fieldPath ) )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'sort:field' on field '" + fieldPath + "'",
						"'sort:field' is not available for fields of this type"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( fieldPath )
				) );
	}

	@Test
	void sort_nativeField_fromLuceneSortField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.extension( LuceneExtension.get() )
						.fromLuceneSortField( new SortField( "nativeField", Type.LONG ) ) )
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIFTH_ID, THIRD_ID, FIRST_ID, SECOND_ID, FOURTH_ID );
	}

	@Test
	void projection_nativeField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Integer> query = scope.query()
				.select( f -> f.field( "nativeField", Integer.class ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( 37 );
	}

	@Test
	@SuppressWarnings("rawtypes")
	void projection_nativeField_withProjectionConverters_enabled() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<ValueWrapper> query = scope.query()
				.select( f -> f.field( "nativeField_converted", ValueWrapper.class ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( new ValueWrapper<>( 37 ) );
	}

	@Test
	void projection_nativeField_withProjectionConverters_disabled() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Integer> query = scope.query()
				.select( f -> f.field( "nativeField_converted", Integer.class, ValueConvert.NO ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( 37 );
	}

	@Test
	void projection_nativeField_unsupportedProjection() {
		StubMappingScope scope = mainIndex.createScope();

		String fieldPath = "nativeField_unsupportedProjection";

		// let's check that it's possible to query the field beforehand
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.extension( LuceneExtension.get() )
						.fromLuceneQuery( new TermQuery( new Term( fieldPath, "37" ) ) )
				)
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID );

		// now, let's check that projecting on the field throws an exception
		assertThatThrownBy( () -> scope.projection()
				.field( fieldPath, Integer.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:field' on field '" + fieldPath + "'",
						"'projection:field' is not available for fields of this type"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( fieldPath )
				) );
	}

	@Test
	void projection_document() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Document> query = scope.query()
				.select(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.where( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetchAll().hits();
		assertThat( result )
				.hasSize( 5 )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "text 1" )
								.hasField( "nativeField", "37" )
								.hasField( "nativeField_converted", "37" )
								.hasField( "nativeField_unsupportedProjection", "37" )
								.hasField( "flattenedObject.stringInObject", "text 2" )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "integer", 2 )
								.hasField( "nativeField", "78" )
								.hasField( "nativeField_converted", "78" )
								.hasField( "nativeField_unsupportedProjection", "78" )
								.hasField( "flattenedObject.integerInObject", 3 )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "nativeField", "13" )
								.hasField( "nativeField_converted", "13" )
								.hasField( "nativeField_unsupportedProjection", "13" )
								.hasField( "geoPoint", toStoredBytes( GeoPoint.of( 40.12, -71.34 ) ) )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "nativeField", "89" )
								.hasField( "nativeField_converted", "89" )
								.hasField( "nativeField_unsupportedProjection", "89" )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "text 2" )
								.hasField( "integer", 1 )
								.hasField( "geoPoint", toStoredBytes( GeoPoint.of( 45.12, -75.34 ) ) )
								.andOnlyInternalFields()
				) );
	}

	/**
	 * Check that the projection on a document includes all fields,
	 * even if there is a field projection, which would usually trigger document filtering.
	 */
	@Test
	void projection_documentAndField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select( f -> f.composite(
						f.extension( LuceneExtension.get() ).document(),
						f.field( "string" )
				)
				)
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<Document> result = query.fetchAll().hits().stream()
				.map( list -> (Document) list.get( 0 ) )
				.collect( Collectors.toList() );
		assertThat( result )
				.hasSize( 1 )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "text 1" )
								.hasField( "nativeField", "37" )
								.hasField( "nativeField_converted", "37" )
								.hasField( "nativeField_unsupportedProjection", "37" )
								.hasField( "flattenedObject.stringInObject", "text 2" )
								.andOnlyInternalFields()
				) );
	}

	@Test
	void projection_explanation() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Explanation> query = scope.query()
				.select( f -> f.extension( LuceneExtension.get() ).explanation() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<Explanation> result = query.fetchAll().hits();
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) )
				.isInstanceOf( Explanation.class )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );
	}

	@Test
	void aggregation_filter_fromLuceneQuery() {
		StubMappingScope scope = mainIndex.createScope();

		AggregationKey<Map<String, Long>> aggregationKey = AggregationKey.of( "agg" );

		SearchQuery<DocumentReference> query = scope.query()
				.extension( LuceneExtension.get() )
				.where( f -> f.matchAll() )
				.aggregation( aggregationKey, f -> f.terms()
						.field( mainIndex.binding().nestedObject.relativeFieldName + ".aggregation1", String.class )
						// The provided predicate factory should already be extended and offer Lucene-specific extensions
						.filter( pf -> pf.fromLuceneQuery( new TermQuery( new Term(
								mainIndex.binding().nestedObject.relativeFieldName + ".discriminator",
								"included"
						) ) ) )
				)
				.toQuery();
		assertThatQuery( query ).aggregation( aggregationKey, agg -> assertThat( agg ).containsExactly(
				entry( "five", 1L ),
				entry( "four", 1L ),
				entry( "one", 1L ),
				entry( "three", 1L ),
				entry( "two", 1L )
		) );
	}

	@Test
	void nativeField_invalidFieldPath() {
		assertThatThrownBy( () -> mainIndex
				.index( FIRST_ID, document -> document.addValue( mainIndex.binding().nativeField_invalidFieldPath, 45 )
				) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid field path; expected path 'nativeField_invalidFieldPath', got 'not the expected path'." );
	}

	@Test
	void backend_unwrap() {
		Backend backend = integration.backend();
		assertThat( backend.unwrap( LuceneBackend.class ) )
				.isNotNull();
	}

	@Test
	void backend_unwrap_error_unknownType() {
		Backend backend = integration.backend();

		assertThatThrownBy( () -> backend.unwrap( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid requested type for this backend: '" + String.class.getName() + "'",
						"Lucene backends can only be unwrapped to '" + LuceneBackend.class.getName() + "'"
				);
	}

	@Test
	void mainIndex_unwrap() {
		IndexManager mainIndexFromIntegration = integration.indexManager( mainIndex.name() );
		assertThat( mainIndexFromIntegration.unwrap( LuceneIndexManager.class ) )
				.isNotNull();
	}

	@Test
	void mainIndex_unwrap_error_unknownType() {
		IndexManager mainIndexFromIntegration = integration.indexManager( mainIndex.name() );

		assertThatThrownBy( () -> mainIndexFromIntegration.unwrap( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid requested type for this index manager: '" + String.class.getName() + "'",
						"Lucene index managers can only be unwrapped to '"
								+ LuceneIndexManager.class.getName() + "'"
				);
	}

	@Test
	void indexReaderAccessor() throws Exception {
		StubMappingScope scope = mainIndex.createScope();

		try ( IndexReader indexReader = scope.extension( LuceneExtension.get() ).openIndexReader() ) {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			TopDocs topDocs = searcher.search( new MatchAllDocsQuery(), 1000 );
			assertThat( topDocs.scoreDocs ).hasSize( 15 );
		}

		scope = mainIndex.createScope( otherIndex );

		try ( IndexReader indexReader = scope.extension( LuceneExtension.get() ).openIndexReader() ) {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			TopDocs topDocs = searcher.search( new MatchAllDocsQuery(), 1000 );
			assertThat( topDocs.scoreDocs ).hasSize( 30 );
		}
	}

	@Test
	void documentProjectionInsideNested() {
		assertThatThrownBy( () -> mainIndex.createScope().query()
				.select( f -> f.object( "nestedObject" ).from(
						f.extension( LuceneExtension.get() ).document()
				).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:document' cannot be nested in an object projection",
						"A document projection represents a root document and adding it as a part of the nested object projection might produce misleading results."
				);
	}

	@Test
	void explanationProjectionInsideNested() {
		assertThatThrownBy( () -> mainIndex.createScope().query()
				.select( f -> f.object( "nestedObject" ).from(
						f.extension( LuceneExtension.get() ).explanation()
				).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:explanation' cannot be nested in an object projection",
						"An explanation projection describes the score computation for the hit and adding it as a part of the nested object projection might produce misleading results."
				);
	}

	@Test
	void documentTree() {
		List<DocumentTree> documentTrees = treeIndex.query().select(
				f -> f.extension( LuceneExtension.get() ).documentTree() )
				.where( f -> f.matchAll() )
				.fetchAllHits();

		assertThat( documentTrees ).hasSize( 1 )
				.satisfiesOnlyOnce( LuceneExtensionIT::assertTreeIndexDocumentTree );
	}

	@Test
	void documentTreeMultipleIndexes() {
		// not really document tree, but let's make sure that we've correctly constructed the documents,
		//  and we can reconstruct back the nesting:
		assertThat(
				someOtherIndex.query().select( f -> f.object( "someOtherNestedObject" ).from( f
						.object( "someOtherNestedObject.someOtherNestedNestedObject" )
						.from( f.field( "someOtherNestedObject.someOtherNestedNestedObject.nestedNestedSomeOtherInteger" )
								.multi() )
						.asList().multi() ).asList().multi() )
						.where( f -> f.match().field( "someOtherString" ).matching( "text 2" ) ).fetchAllHits()
		).containsExactly(
				List.of(
						List.of( List.of() ),
						List.of( List.of( List.of( List.of( 211, 212, 213 ) ), List.of( List.of( 221, 222 ) ) ) ),
						List.of( List.of( List.of( List.of( 311, 312, 313 ) ), List.of( List.of( 321, 322 ) ),
								List.of( List.of( 331, 332 ) ) ) ) )
		);

		// now test the tree structure:
		List<DocumentTree> documentTrees = treeIndex.createScope( someOtherIndex )
				.query().select( f -> f.extension( LuceneExtension.get() ).documentTree() )
				.where( f -> f.matchAll() )
				.fetchAllHits();

		assertThat( documentTrees ).hasSize( 3 )
				.satisfiesOnlyOnce( LuceneExtensionIT::assertTreeIndexDocumentTree )
				.satisfiesOnlyOnce( rootTree -> {
					// first doc from someOtherIndex
					assertThatDocument( rootTree.document() )
							.hasField( "someOtherString", "text 1" )
							.hasField( "someOtherInteger", 555 )
							.andOnlyInternalFields();

					assertThat( rootTree.nested() )
							.containsOnlyKeys( "someOtherNestedObject" );
					Collection<DocumentTree> someOtherNestedObjects = rootTree.nested().get( "someOtherNestedObject" );
					assertThat( someOtherNestedObjects )
							.hasSize( 1 )
							.satisfiesOnlyOnce( someOtherNestedObject -> {
								assertThatDocument( someOtherNestedObject.document() )
										.hasField( "someOtherNestedObject.someOtherInteger", 456 )
										.andOnlyInternalFields();

								assertThat( someOtherNestedObject.nested() )
										.isEmpty();
							} );
				} )
				.satisfiesOnlyOnce( rootTree -> {
					// second doc from someOtherIndex
					assertThatDocument( rootTree.document() )
							.hasField( "someOtherString", "text 2" )
							.hasField( "someOtherInteger", 2 )
							.andOnlyInternalFields();

					assertThat( rootTree.nested() )
							.containsOnlyKeys( "someOtherNestedObject" );
					Collection<DocumentTree> someOtherNestedObjects = rootTree.nested().get( "someOtherNestedObject" );
					assertThat( someOtherNestedObjects )
							.hasSize( 3 )
							// nested1
							.satisfiesOnlyOnce( someOtherNestedObject -> {
								assertThatDocument( someOtherNestedObject.document() )
										.hasField( "someOtherNestedObject.someOtherInteger", 11, 12, 13 )
										.andOnlyInternalFields();

								assertThat( someOtherNestedObject.nested() )
										.isEmpty();
							} )
							// nested2
							.satisfiesOnlyOnce( someOtherNestedObject -> {
								assertThatDocument( someOtherNestedObject.document() )
										.hasField( "someOtherNestedObject.someOtherInteger", 21, 22 )
										.andOnlyInternalFields();

								assertThat( someOtherNestedObject.nested() )
										.containsOnlyKeys( "someOtherNestedNestedObject" );
								Collection<DocumentTree> someOtherNestedNestedObjects =
										someOtherNestedObject.nested().get( "someOtherNestedNestedObject" );
								assertThat( someOtherNestedNestedObjects )
										.hasSize( 2 )
										.satisfiesOnlyOnce( someOtherNestedNestedObject -> {
											assertThatDocument( someOtherNestedNestedObject.document() )
													.hasField(
															"someOtherNestedObject.someOtherNestedNestedObject.nestedNestedSomeOtherInteger",
															211, 212, 213 )
													.andOnlyInternalFields();

											assertThat( someOtherNestedNestedObject.nested() )
													.isEmpty();
										} )
										.satisfiesOnlyOnce( someOtherNestedNestedObject -> {
											assertThatDocument( someOtherNestedNestedObject.document() )
													.hasField(
															"someOtherNestedObject.someOtherNestedNestedObject.nestedNestedSomeOtherInteger",
															221, 222 )
													.andOnlyInternalFields();

											assertThat( someOtherNestedNestedObject.nested() )
													.isEmpty();
										} );

							} )
							// nested3
							.satisfiesOnlyOnce( someOtherNestedObject -> {
								assertThatDocument( someOtherNestedObject.document() )
										.hasField( "someOtherNestedObject.someOtherInteger", 31 )
										.andOnlyInternalFields();

								assertThat( someOtherNestedObject.nested() )
										.containsOnlyKeys( "someOtherNestedNestedObject" );
								Collection<DocumentTree> someOtherNestedNestedObjects =
										someOtherNestedObject.nested().get( "someOtherNestedNestedObject" );
								assertThat( someOtherNestedNestedObjects )
										.hasSize( 3 )
										.satisfiesOnlyOnce( someOtherNestedNestedObject -> {
											assertThatDocument( someOtherNestedNestedObject.document() )
													.hasField(
															"someOtherNestedObject.someOtherNestedNestedObject.nestedNestedSomeOtherInteger",
															311, 312, 313 )
													.andOnlyInternalFields();

											assertThat( someOtherNestedNestedObject.nested() )
													.isEmpty();
										} )
										.satisfiesOnlyOnce( someOtherNestedNestedObject -> {
											assertThatDocument( someOtherNestedNestedObject.document() )
													.hasField(
															"someOtherNestedObject.someOtherNestedNestedObject.nestedNestedSomeOtherInteger",
															321, 322 )
													.andOnlyInternalFields();

											assertThat( someOtherNestedNestedObject.nested() )
													.isEmpty();
										} )
										.satisfiesOnlyOnce( someOtherNestedNestedObject -> {
											assertThatDocument( someOtherNestedNestedObject.document() )
													.hasField(
															"someOtherNestedObject.someOtherNestedNestedObject.nestedNestedSomeOtherInteger",
															331, 332 )
													.andOnlyInternalFields();

											assertThat( someOtherNestedNestedObject.nested() )
													.isEmpty();
										} );
							} );
				} );
	}

	private static void assertTreeIndexDocumentTree(DocumentTree rootTree) {
		assertThatDocument( rootTree.document() )
				.hasField( "string", "text 1" )
				.hasField( "nativeField", "37" )
				.hasField( "nativeField_converted", "37" )
				.hasField( "nativeField_unsupportedProjection", "37" )
				.hasField( "flattenedObject.stringInObject", "text 2" )
				.hasField( "flattenedObject.flattenedObject2.integerInObject", 4 )
				.hasField( "flattenedObject.flattenedObject2.flattenedObject3.integerInObject", 6 )
				.andOnlyInternalFields();

		assertThat( rootTree.nested() )
				.containsOnlyKeys( "nestedObject" );
		Collection<DocumentTree> nestedObjects = rootTree.nested().get( "nestedObject" );
		assertThat( nestedObjects )
				.hasSize( 2 )
				.satisfiesOnlyOnce( nestedObject -> {
					assertThatDocument( nestedObject.document() )
							.andOnlyInternalFields();
					assertThat( nestedObject.nested() ).isEmpty();
				} )
				.satisfiesOnlyOnce( nestedObject -> {
					assertThatDocument( nestedObject.document() )
							.hasField( "nestedObject.integerInObject", 123 )
							.andOnlyInternalFields();
					assertThat( nestedObject.nested() ).containsKeys( "nestedObject2" );
					Collection<DocumentTree> nestedObject2s = nestedObject.nested().get( "nestedObject2" );
					assertThat( nestedObject2s )
							.hasSize( 2 )
							.satisfiesOnlyOnce( nestedObject2 -> {
								assertThatDocument( nestedObject2.document() )
										.hasField( "nestedObject.nestedObject2.integerInObject", 1 )
										.andOnlyInternalFields();
								assertThat( nestedObject2.nested() ).isEmpty();
							} )
							.satisfiesOnlyOnce( nestedObject2 -> {
								assertThatDocument( nestedObject2.document() )
										.hasField( "nestedObject.nestedObject2.integerInObject", 2 )
										.andOnlyInternalFields();
								assertThat( nestedObject2.nested() ).containsKeys( "nestedObject3" );
								assertThat( nestedObject2.nested().get( "nestedObject3" ) )
										.hasSize( 1 )
										.satisfiesOnlyOnce( nestedObject3 -> {
											assertThatDocument( nestedObject3.document() )
													.hasField( "nestedObject.nestedObject2.nestedObject3.integerInObject", 20 )
													.andOnlyInternalFields();
											assertThat( nestedObject3.nested() ).isEmpty();
										} );
							} );


				} );
	}

	private void initData() {
		indexDataSet( mainIndex );

		// Use the same IDs and dataset for otherIndex.binding() to trigger
		// a failure in explain() tests if index selection doesn't work correctly.
		indexDataSet( otherIndex );
		indexDataSetDocumentTree( treeIndex );
		indexDataSetSomeOtherDocumentTree( someOtherIndex );

		// Check that all documents are searchable
		assertThatQuery( mainIndex.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasDocRefHitsAnyOrder(
						mainIndex.typeName(),
						FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
				);
		assertThatQuery( otherIndex.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasDocRefHitsAnyOrder(
						otherIndex.typeName(),
						FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
				);
	}

	private static byte[] toStoredBytes(GeoPoint geoPoint) {
		byte[] bytes = new byte[2 * Double.BYTES];
		DoublePoint.encodeDimension( geoPoint.latitude(), bytes, 0 );
		DoublePoint.encodeDimension( geoPoint.longitude(), bytes, Double.BYTES );
		return bytes;
	}

	private static void indexDataSet(SimpleMappedIndex<IndexBinding> index) {
		index.bulkIndexer()
				.add( FIRST_ID, document -> {
					document.addValue( index.binding().string, "text 1" );

					document.addValue( index.binding().nativeField, 37 );
					document.addValue( index.binding().nativeField_converted, 37 );
					document.addValue( index.binding().nativeField_unsupportedProjection, 37 );

					document.addValue( index.binding().sort1, "a" );
					document.addValue( index.binding().sort2, "z" );
					document.addValue( index.binding().sort3, "z" );

					DocumentElement nestedObject1 = document.addObject( index.binding().nestedObject.self );
					nestedObject1.addValue( index.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( index.binding().nestedObject.sort1, "a" );
					nestedObject1.addValue( index.binding().nestedObject.aggregation1, "one" );
					DocumentElement nestedObject2 = document.addObject( index.binding().nestedObject.self );
					nestedObject2.addValue( index.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( index.binding().nestedObject.sort1, "b" );
					nestedObject2.addValue( index.binding().nestedObject.aggregation1, "fifty-one" );

					DocumentElement flattenedObject1 = document.addObject( index.binding().flattenedObject.self );
					flattenedObject1.addValue( index.binding().flattenedObject.stringInObject, "text 2" );
					flattenedObject1.addValue( index.binding().flattenedObject.sortInObject, "1" );
				} )
				.add( SECOND_ID, document -> {
					document.addValue( index.binding().integer, 2 );

					document.addValue( index.binding().nativeField, 78 );
					document.addValue( index.binding().nativeField_converted, 78 );
					document.addValue( index.binding().nativeField_unsupportedProjection, 78 );

					document.addValue( index.binding().sort1, "z" );
					document.addValue( index.binding().sort2, "a" );
					document.addValue( index.binding().sort3, "z" );

					DocumentElement nestedObject1 = document.addObject( index.binding().nestedObject.self );
					nestedObject1.addValue( index.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( index.binding().nestedObject.sort1, "b" );
					nestedObject1.addValue( index.binding().nestedObject.aggregation1, "two" );
					DocumentElement nestedObject2 = document.addObject( index.binding().nestedObject.self );
					nestedObject2.addValue( index.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( index.binding().nestedObject.sort1, "a" );
					nestedObject2.addValue( index.binding().nestedObject.aggregation1, "fifty-two" );

					DocumentElement flattenedObject1 = document.addObject( index.binding().flattenedObject.self );
					flattenedObject1.addValue( index.binding().flattenedObject.integerInObject, 3 );
					flattenedObject1.addValue( index.binding().flattenedObject.sortInObject, "2" );
				} )
				.add( THIRD_ID, document -> {
					document.addValue( index.binding().geoPoint, GeoPoint.of( 40.12, -71.34 ) );

					document.addValue( index.binding().nativeField, 13 );
					document.addValue( index.binding().nativeField_converted, 13 );
					document.addValue( index.binding().nativeField_unsupportedProjection, 13 );

					document.addValue( index.binding().sort1, "z" );
					document.addValue( index.binding().sort2, "z" );
					document.addValue( index.binding().sort3, "a" );

					DocumentElement nestedObject1 = document.addObject( index.binding().nestedObject.self );
					nestedObject1.addValue( index.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( index.binding().nestedObject.sort1, "c" );
					nestedObject1.addValue( index.binding().nestedObject.aggregation1, "three" );
					DocumentElement nestedObject2 = document.addObject( index.binding().nestedObject.self );
					nestedObject2.addValue( index.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( index.binding().nestedObject.sort1, "b" );
					nestedObject2.addValue( index.binding().nestedObject.aggregation1, "fifty-three" );

					DocumentElement flattenedObject1 = document.addObject( index.binding().flattenedObject.self );
					flattenedObject1.addValue( index.binding().flattenedObject.sortInObject, "3" );
				} )
				.add( FOURTH_ID, document -> {
					document.addValue( index.binding().nativeField, 89 );
					document.addValue( index.binding().nativeField_converted, 89 );
					document.addValue( index.binding().nativeField_unsupportedProjection, 89 );

					document.addValue( index.binding().sort1, "z" );
					document.addValue( index.binding().sort2, "z" );
					document.addValue( index.binding().sort3, "z" );

					DocumentElement nestedObject1 = document.addObject( index.binding().nestedObject.self );
					nestedObject1.addValue( index.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( index.binding().nestedObject.sort1, "d" );
					nestedObject1.addValue( index.binding().nestedObject.aggregation1, "four" );
					DocumentElement nestedObject2 = document.addObject( index.binding().nestedObject.self );
					nestedObject2.addValue( index.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( index.binding().nestedObject.sort1, "c" );
					nestedObject2.addValue( index.binding().nestedObject.aggregation1, "fifty-four" );

					DocumentElement flattenedObject1 = document.addObject( index.binding().flattenedObject.self );
					flattenedObject1.addValue( index.binding().flattenedObject.sortInObject, "4" );
				} )
				.add( FIFTH_ID, document -> {
					// This document should not match any query
					document.addValue( index.binding().string, "text 2" );
					document.addValue( index.binding().integer, 1 );
					document.addValue( index.binding().geoPoint, GeoPoint.of( 45.12, -75.34 ) );

					document.addValue( index.binding().sort1, "zz" );
					document.addValue( index.binding().sort2, "zz" );
					document.addValue( index.binding().sort3, "zz" );

					DocumentElement nestedObject1 = document.addObject( index.binding().nestedObject.self );
					nestedObject1.addValue( index.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( index.binding().nestedObject.sort1, "e" );
					nestedObject1.addValue( index.binding().nestedObject.aggregation1, "five" );
					DocumentElement nestedObject2 = document.addObject( index.binding().nestedObject.self );
					nestedObject2.addValue( index.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( index.binding().nestedObject.sort1, "a" );
					nestedObject2.addValue( index.binding().nestedObject.aggregation1, "fifty-five" );

					DocumentElement flattenedObject1 = document.addObject( index.binding().flattenedObject.self );
					flattenedObject1.addValue( index.binding().flattenedObject.sortInObject, "5" );
				} )
				.join();
	}

	private static void indexDataSetDocumentTree(SimpleMappedIndex<IndexBinding> index) {
		index.bulkIndexer()
				.add( FIRST_ID, document -> {
					document.addValue( index.binding().string, "text 1" );
					document.addValue( index.binding().nativeField, 37 );
					document.addValue( index.binding().nativeField_converted, 37 );
					document.addValue( index.binding().nativeField_unsupportedProjection, 37 );

					// won't show any fields
					DocumentElement nestedObject1 = document.addObject( index.binding().nestedObject.self );
					nestedObject1.addValue( index.binding().nestedObject.discriminator, "included" );

					DocumentElement nestedObject2 = document.addObject( index.binding().nestedObject.self );
					nestedObject2.addValue( index.binding().nestedObject.integerInObject, 123 );

					DocumentElement nestedObject2x1 = nestedObject2.addObject( index.binding().nestedObject.nestedObject.self );
					nestedObject2x1.addValue( index.binding().nestedObject.nestedObject.integerInObject, 1 );

					DocumentElement nestedObject2x2 = nestedObject2.addObject( index.binding().nestedObject.nestedObject.self );
					nestedObject2x2.addValue( index.binding().nestedObject.nestedObject.integerInObject, 2 );

					DocumentElement nestedObject2x2nested =
							nestedObject2x2.addObject( index.binding().nestedObject.nestedObject.nestedObject.self );
					nestedObject2x2nested.addValue( index.binding().nestedObject.nestedObject.nestedObject.integerInObject,
							20 );

					DocumentElement flattenedObject1 = document.addObject( index.binding().flattenedObject.self );
					flattenedObject1.addValue( index.binding().flattenedObject.stringInObject, "text 2" );
					flattenedObject1.addValue( index.binding().flattenedObject.sortInObject, "1" );

					DocumentElement flattenedObject1nested =
							flattenedObject1.addObject( index.binding().flattenedObject.nestedObject.self );
					flattenedObject1nested.addValue( index.binding().flattenedObject.nestedObject.integerInObject, 3 );

					DocumentElement flattenedObject1flattened =
							flattenedObject1.addObject( index.binding().flattenedObject.flattenedObject.self );
					flattenedObject1flattened.addValue( index.binding().flattenedObject.flattenedObject.integerInObject, 4 );

					DocumentElement flattenedObject1x2nested =
							flattenedObject1nested.addObject( index.binding().flattenedObject.nestedObject.nestedObject.self );
					flattenedObject1x2nested
							.addValue( index.binding().flattenedObject.nestedObject.nestedObject.integerInObject, 5 );

					DocumentElement flattenedObject1x2flattened = flattenedObject1flattened
							.addObject( index.binding().flattenedObject.flattenedObject.flattenedObject.self );
					flattenedObject1x2flattened
							.addValue( index.binding().flattenedObject.flattenedObject.flattenedObject.integerInObject, 6 );

				} )
				.join();
	}

	private static void indexDataSetSomeOtherDocumentTree(SimpleMappedIndex<SomeOtherIndexBinding> index) {
		index.bulkIndexer()
				.add( FIRST_ID, document -> {
					document.addValue( index.binding().someOtherString, "text 1" );
					document.addValue( index.binding().someOtherInteger, 555 );

					DocumentElement nested = document.addObject( index.binding().someOtherNestedObject );
					nested.addValue( index.binding().nestedSomeOtherInteger, 456 );
				} )
				.add( SECOND_ID, document -> {
					document.addValue( index.binding().someOtherString, "text 2" );
					document.addValue( index.binding().someOtherInteger, 2 );

					DocumentElement nested1 = document.addObject( index.binding().someOtherNestedObject );
					nested1.addValue( index.binding().nestedSomeOtherInteger, 11 );
					nested1.addValue( index.binding().nestedSomeOtherInteger, 12 );
					nested1.addValue( index.binding().nestedSomeOtherInteger, 13 );

					DocumentElement nested2 = document.addObject( index.binding().someOtherNestedObject );
					nested2.addValue( index.binding().nestedSomeOtherInteger, 21 );
					nested2.addValue( index.binding().nestedSomeOtherInteger, 22 );

					DocumentElement nested21 = nested2.addObject( index.binding().someOtherNestedNestedObject );
					nested21.addValue( index.binding().nestedNestedSomeOtherInteger, 211 );
					nested21.addValue( index.binding().nestedNestedSomeOtherInteger, 212 );
					nested21.addValue( index.binding().nestedNestedSomeOtherInteger, 213 );

					DocumentElement nested22 = nested2.addObject( index.binding().someOtherNestedNestedObject );
					nested22.addValue( index.binding().nestedNestedSomeOtherInteger, 221 );
					nested22.addValue( index.binding().nestedNestedSomeOtherInteger, 222 );

					DocumentElement nested3 = document.addObject( index.binding().someOtherNestedObject );
					nested3.addValue( index.binding().nestedSomeOtherInteger, 31 );

					DocumentElement nested31 = nested3.addObject( index.binding().someOtherNestedNestedObject );
					nested31.addValue( index.binding().nestedNestedSomeOtherInteger, 311 );
					nested31.addValue( index.binding().nestedNestedSomeOtherInteger, 312 );
					nested31.addValue( index.binding().nestedNestedSomeOtherInteger, 313 );

					DocumentElement nested32 = nested3.addObject( index.binding().someOtherNestedNestedObject );
					nested32.addValue( index.binding().nestedNestedSomeOtherInteger, 321 );
					nested32.addValue( index.binding().nestedNestedSomeOtherInteger, 322 );

					DocumentElement nested33 = nested3.addObject( index.binding().someOtherNestedNestedObject );
					nested33.addValue( index.binding().nestedNestedSomeOtherInteger, 331 );
					nested33.addValue( index.binding().nestedNestedSomeOtherInteger, 332 );

				} ).join();
	}


	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;
		final IndexFieldReference<GeoPoint> geoPoint;
		final IndexFieldReference<Integer> nativeField;
		final IndexFieldReference<Integer> nativeField_converted;
		final IndexFieldReference<Integer> nativeField_unsupportedProjection;
		final IndexFieldReference<Integer> nativeField_invalidFieldPath;

		final IndexFieldReference<String> sort1;
		final IndexFieldReference<String> sort2;
		final IndexFieldReference<String> sort3;

		final ObjectMapping nestedObject;
		final ObjectMapping flattenedObject;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field(
					"integer",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.toReference();
			string = root.field(
					"string",
					f -> f.asString().projectable( Projectable.YES )
			)
					.toReference();
			geoPoint = root.field(
					"geoPoint",
					f -> f.asGeoPoint().projectable( Projectable.YES )
			)
					.toReference();
			nativeField = root.field(
					"nativeField",
					f -> f.extension( LuceneExtension.get() )
							.asNative( Integer.class, LuceneExtensionIT::contributeNativeField,
									LuceneExtensionIT::fromNativeField )
			)
					.toReference();
			nativeField_converted = root.field(
					"nativeField_converted",
					f -> f.extension( LuceneExtension.get() )
							.asNative( Integer.class, LuceneExtensionIT::contributeNativeField,
									LuceneExtensionIT::fromNativeField )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() )
			)
					.toReference();
			nativeField_unsupportedProjection = root.field(
					"nativeField_unsupportedProjection",
					f -> f.extension( LuceneExtension.get() )
							.asNative( Integer.class, LuceneExtensionIT::contributeNativeField )
			)
					.toReference();
			nativeField_invalidFieldPath = root.field(
					"nativeField_invalidFieldPath",
					f -> f.extension( LuceneExtension.get() )
							.asNative( Integer.class, LuceneExtensionIT::contributeNativeFieldInvalidFieldPath )
			)
					.toReference();

			sort1 = root.field( "sort1", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
			sort2 = root.field( "sort2", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
			sort3 = root.field( "sort3", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();

			nestedObject = ObjectMapping.create( root, "nestedObject", ObjectStructure.NESTED, true, 2 );
			flattenedObject = ObjectMapping.create( root, "flattenedObject", ObjectStructure.FLATTENED, true, 2 );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;
		// Use different names from the root, otherwise the tests might miss some bugs.
		final IndexFieldReference<String> sort1;
		final IndexFieldReference<String> aggregation1;
		final IndexFieldReference<Integer> integerInObject;
		final IndexFieldReference<String> stringInObject;
		final IndexFieldReference<String> sortInObject;
		final ObjectMapping nestedObject;
		final ObjectMapping flattenedObject;

		public static ObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure,
				boolean multiValued, int nestLevel) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			if ( multiValued ) {
				objectField.multiValued();
			}
			return new ObjectMapping( relativeFieldName, objectField, nestLevel );
		}

		private ObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField, int nestLevel) {
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();

			discriminator = objectField.field( "discriminator", f -> f.asString() ).toReference();

			sort1 = objectField.field( "sort1", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
			aggregation1 = objectField.field( "aggregation1", f -> f.asString().aggregable( Aggregable.YES ) )
					.toReference();
			integerInObject = objectField.field( "integerInObject", f -> f.asInteger().projectable( Projectable.YES ) )
					.toReference();
			stringInObject = objectField.field( "stringInObject", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
			sortInObject = objectField.field( "sortInObject", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();

			if ( nestLevel < 4 ) {
				nestedObject = ObjectMapping.create(
						objectField, "nestedObject" + nestLevel, ObjectStructure.NESTED, true, nestLevel + 1 );
				flattenedObject = ObjectMapping.create(
						objectField, "flattenedObject" + nestLevel, ObjectStructure.FLATTENED, true, nestLevel + 1 );
			}
			else {
				nestedObject = null;
				flattenedObject = null;
			}
		}
	}

	private static class SomeOtherIndexBinding {
		final IndexFieldReference<Integer> someOtherInteger;
		final IndexFieldReference<String> someOtherString;
		private final IndexObjectFieldReference someOtherNestedObject;
		private final IndexFieldReference<Integer> nestedSomeOtherInteger;

		private final IndexObjectFieldReference someOtherNestedNestedObject;
		private final IndexFieldReference<Integer> nestedNestedSomeOtherInteger;

		SomeOtherIndexBinding(IndexSchemaElement root) {
			someOtherInteger = root.field(
					"someOtherInteger",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.toReference();
			someOtherString = root.field(
					"someOtherString",
					f -> f.asString().projectable( Projectable.YES )
			)
					.toReference();
			IndexSchemaObjectField nested = root.objectField( "someOtherNestedObject", ObjectStructure.NESTED )
					.multiValued();
			nestedSomeOtherInteger = nested.field(
					"someOtherInteger",
					f -> f.asInteger().projectable( Projectable.YES )
			).multiValued().toReference();

			someOtherNestedObject = nested.toReference();


			IndexSchemaObjectField nestedNested = nested.objectField( "someOtherNestedNestedObject", ObjectStructure.NESTED )
					.multiValued();
			nestedNestedSomeOtherInteger = nestedNested.field(
					"nestedNestedSomeOtherInteger",
					f -> f.asInteger().projectable( Projectable.YES )
			).multiValued().toReference();

			someOtherNestedNestedObject = nestedNested.toReference();
		}
	}

	private static void contributeNativeField(String absoluteFieldPath, Integer value, Consumer<IndexableField> collector) {
		collector.accept( new StringField( absoluteFieldPath, value.toString(), Store.YES ) );
		collector.accept( new NumericDocValuesField( absoluteFieldPath, value.longValue() ) );
	}

	private static Integer fromNativeField(IndexableField field) {
		return Integer.parseInt( field.stringValue() );
	}

	private static void contributeNativeFieldInvalidFieldPath(String absoluteFieldPath, Integer value,
			Consumer<IndexableField> collector) {
		collector.accept( new StringField( "not the expected path", value.toString(), Store.YES ) );
	}
}
