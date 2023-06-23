/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;

import java.util.ArrayList;
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

public class LuceneExtensionIT {

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String FOURTH_ID = "4";
	private static final String FIFTH_ID = "5";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<IndexBinding> otherIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "other" );

	private SearchIntegration integration;

	@Before
	public void setup() {
		this.integration = setupHelper.start().withIndexes( mainIndex, otherIndex ).setup().integration();

		initData();
	}

	@Test
	@SuppressWarnings("unused")
	public void queryContext() {
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
	public void query() {
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
	public void query_topDocs() {
		StubMappingScope scope = mainIndex.createScope();

		LuceneSearchResult<DocumentReference> result = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.matchAll() )
				.fetchAll();

		assertThat( result.topDocs() ).isNotNull();
	}

	@Test
	public void query_explain_singleIndex() {
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
	public void query_explain_singleIndex_invalidId() {
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
	public void query_explain_multipleIndexes() {
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
	public void query_explain_multipleIndexes_missingTypeName() {
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
	public void query_explain_multipleIndexes_invalidIndexName() {
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
	public void scroll_onFetchable() {
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
	public void scroll_onQuery() {
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
	public void predicate_fromLuceneQuery() {
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
	public void predicate_fromLuceneQuery_separatePredicate() {
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
	public void predicate_fromLuceneQuery_withRoot() {
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
	public void sort_fromLuceneSortField() {
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
	public void sort_fromLuceneSortField_separateSort() {
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
	public void sort_fromLuceneSortField_withRoot() {
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
	public void sort_filter_fromLuceneQuery() {
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
	public void predicate_nativeField() {
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
	public void predicate_nativeField_fromLuceneQuery() {
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
	public void predicate_nativeField_exists() {
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
	public void sort_nativeField() {
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
	public void sort_nativeField_fromLuceneSortField() {
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
	public void projection_nativeField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Integer> query = scope.query()
				.select( f -> f.field( "nativeField", Integer.class ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( 37 );
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void projection_nativeField_withProjectionConverters_enabled() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<ValueWrapper> query = scope.query()
				.select( f -> f.field( "nativeField_converted", ValueWrapper.class ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( new ValueWrapper<>( 37 ) );
	}

	@Test
	public void projection_nativeField_withProjectionConverters_disabled() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<Integer> query = scope.query()
				.select( f -> f.field( "nativeField_converted", Integer.class, ValueConvert.NO ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( 37 );
	}

	@Test
	public void projection_nativeField_unsupportedProjection() {
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
	public void projection_document() {
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
	public void projection_documentAndField() {
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
	public void projection_explanation() {
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
	public void aggregation_filter_fromLuceneQuery() {
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
	public void nativeField_invalidFieldPath() {
		assertThatThrownBy( () -> mainIndex
				.index( FIRST_ID, document -> document.addValue( mainIndex.binding().nativeField_invalidFieldPath, 45 )
				) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid field path; expected path 'nativeField_invalidFieldPath', got 'not the expected path'." );
	}

	@Test
	public void backend_unwrap() {
		Backend backend = integration.backend();
		assertThat( backend.unwrap( LuceneBackend.class ) )
				.isNotNull();
	}

	@Test
	public void backend_unwrap_error_unknownType() {
		Backend backend = integration.backend();

		assertThatThrownBy( () -> backend.unwrap( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid requested type for this backend: '" + String.class.getName() + "'",
						"Lucene backends can only be unwrapped to '" + LuceneBackend.class.getName() + "'"
				);
	}

	@Test
	public void mainIndex_unwrap() {
		IndexManager mainIndexFromIntegration = integration.indexManager( mainIndex.name() );
		assertThat( mainIndexFromIntegration.unwrap( LuceneIndexManager.class ) )
				.isNotNull();
	}

	@Test
	public void mainIndex_unwrap_error_unknownType() {
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
	public void indexReaderAccessor() throws Exception {
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
	public void documentProjectionInsideNested() {
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
	public void explanationProjectionInsideNested() {
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

	private void initData() {
		indexDataSet( mainIndex );

		// Use the same IDs and dataset for otherIndex.binding() to trigger
		// a failure in explain() tests if index selection doesn't work correctly.
		indexDataSet( otherIndex );

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

			nestedObject = ObjectMapping.create( root, "nestedObject", ObjectStructure.NESTED, true );
			flattenedObject = ObjectMapping.create( root, "flattenedObject", ObjectStructure.FLATTENED, true );
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

		public static ObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure,
				boolean multiValued) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			if ( multiValued ) {
				objectField.multiValued();
			}
			return new ObjectMapping( relativeFieldName, objectField );
		}

		private ObjectMapping(String relativeFieldName, IndexSchemaObjectField objectField) {
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
