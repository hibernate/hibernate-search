/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene;

import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.assertj.core.api.Assertions;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryOptionsStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryWhereStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryHitTypeStep;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LuceneExtensionIT {

	private static final String BACKEND_NAME = "myLuceneBackend";
	private static final String INDEX_NAME = "IndexName";
	private static final String OTHER_INDEX_NAME = "OtherIndexName";

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String FOURTH_ID = "4";
	private static final String FIFTH_ID = "5";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SearchIntegration integration;

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IndexMapping otherIndexMapping;
	private StubMappingIndexManager otherIndexManager;

	@Before
	public void setup() {
		this.integration = setupHelper.start( BACKEND_NAME )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						OTHER_INDEX_NAME,
						ctx -> this.otherIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.otherIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	@SuppressWarnings("unused")
	public void queryContext() {
		StubMappingScope scope = indexManager.createScope();

		// Put intermediary contexts into variables to check they have the right type
		LuceneSearchQueryHitTypeStep<DocumentReference, DocumentReference, StubLoadingOptionsStep> context1 =
				scope.query().extension( LuceneExtension.get() );
		LuceneSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> context2 = context1.asProjection(
				f -> f.composite(
						// We don't care about the document, it's just to test that the factory context allows Lucene-specific projection
						(docRef, document) -> docRef,
						f.documentReference(), f.document()
				)
		);
		// Note we can use Lucene-specific predicates immediately
		LuceneSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> context3 =
				context2.where( f -> f.fromLuceneQuery( new MatchAllDocsQuery() ) );
		// Note we can use Lucene-specific sorts immediately
		LuceneSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> context4 =
				context3.sort( f -> f.fromLuceneSortField( new SortField( "sort1", Type.STRING ) ) );

		// Put the query and result into variables to check they have the right type
		LuceneSearchQuery<DocumentReference> query = context4.toQuery();
		LuceneSearchResult<DocumentReference> result = query.fetchAll();

		assertThat( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID )
				.hasTotalHitCount( 5 );

		// Also check (at compile time) the context type for other asXXX() methods, since we need to override each method explicitly
		LuceneSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> asReferenceContext =
				scope.query().extension( LuceneExtension.get() ).asEntityReference();
		LuceneSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> asEntityContext =
				scope.query().extension( LuceneExtension.get() ).asEntity();
		SearchProjection<DocumentReference> projection = scope.projection().documentReference().toProjection();
		LuceneSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> asProjectionContext =
				scope.query().extension( LuceneExtension.get() ).asProjection( projection );
		LuceneSearchQueryWhereStep<List<?>, ?> asProjectionsContext =
				scope.query().extension( LuceneExtension.get() ).asProjections( projection, projection );
		LuceneSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> defaultResultContext =
				scope.query().extension( LuceneExtension.get() )
						.where( f -> f.fromLuceneQuery( new MatchAllDocsQuery() ) );
	}

	@Test
	public void query() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> genericQuery = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Put the query and result into variables to check they have the right type
		LuceneSearchQuery<DocumentReference> query = genericQuery.extension( LuceneExtension.get() );
		LuceneSearchResult<DocumentReference> result = query.fetchAll();
		assertThat( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID )
				.hasTotalHitCount( 5 );

		// Unsupported extension
		SubTest.expectException(
				() -> query.extension( (SearchQuery<DocumentReference> original, LoadingContext<?, ?> loadingContext) -> Optional.empty() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void query_explain_singleIndex() {
		StubMappingScope scope = indexManager.createScope();

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		Assertions.assertThat( query.explain( FIRST_ID ) )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );

		// Non-matching document
		Assertions.assertThat( query.explain( FIFTH_ID ) )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );
	}

	@Test
	public void query_explain_singleIndex_invalidId() {
		StubMappingScope scope = indexManager.createScope();

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Non-existing document
		SubTest.expectException(
				() -> query.explain( "InvalidId" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Document with id 'InvalidId' does not exist in index '" + INDEX_NAME + "'"
				);
	}

	@Test
	public void query_explain_multipleIndexes() {
		StubMappingScope scope = indexManager.createScope( otherIndexManager );

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		Assertions.assertThat( query.explain( INDEX_NAME, FIRST_ID ) )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );

		// Non-matching document
		Assertions.assertThat( query.explain( INDEX_NAME, FIFTH_ID ) )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );
	}

	@Test
	public void query_explain_multipleIndexes_missingIndexName() {
		StubMappingScope scope = indexManager.createScope( otherIndexManager );

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		SubTest.expectException(
				() -> query.explain( FIRST_ID )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "explain(String id) cannot be used when the query targets multiple indexes" )
				.hasMessageContaining(
						"pass one of [" + INDEX_NAME + ", " + OTHER_INDEX_NAME + "]"
				);
	}

	@Test
	public void query_explain_multipleIndexes_invalidIndexName() {
		StubMappingScope scope = indexManager.createScope( otherIndexManager );

		LuceneSearchQuery<DocumentReference> query = scope.query().extension( LuceneExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		SubTest.expectException(
				() -> query.explain( "NotAnIndexName", FIRST_ID )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"index name 'NotAnIndexName' is not among the indexes targeted by this query: ["
								+ INDEX_NAME + ", " + OTHER_INDEX_NAME + "]"
				);
	}

	@Test
	public void predicate_fromLuceneQuery() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.extension( LuceneExtension.get() )
								.fromLuceneQuery( new TermQuery( new Term( "string", "text 1" ) ) )
						)
						.should( f.extension( LuceneExtension.get() )
								.fromLuceneQuery( IntPoint.newExactQuery( "integer", 2 ) )
						)
						.should( f.extension( LuceneExtension.get() )
								.fromLuceneQuery( LatLonPoint.newDistanceQuery( "geoPoint", 40, -70, 200_000 ) )
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID )
				.hasTotalHitCount( 3 );
	}

	@Test
	public void predicate_fromLuceneQuery_separatePredicate() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate1 = scope.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( new TermQuery( new Term( "string", "text 1" ) ) ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( IntPoint.newExactQuery( "integer", 2 ) ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().extension( LuceneExtension.get() )
				.fromLuceneQuery( LatLonPoint.newDistanceQuery( "geoPoint", 40, -70, 200_000 ) ).toPredicate();
		SearchPredicate booleanPredicate = scope.predicate().bool()
				.should( predicate1 )
				.should( predicate2 )
				.should( predicate3 )
				.toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where( booleanPredicate )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID )
				.hasTotalHitCount( 3 );
	}

	@Test
	public void sort_fromLuceneSortField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f
						.extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort1", Type.STRING ) )
						.then().extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort2", Type.STRING ) )
						.then().extension( LuceneExtension.get() )
								.fromLuceneSortField( new SortField( "sort3", Type.STRING ) )
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
		);

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f
						.extension().ifSupported(
								LuceneExtension.get(),
								c2 -> c2.fromLuceneSort( new Sort(
										new SortField( "sort3", Type.STRING ),
										new SortField( "sort2", Type.STRING ),
										new SortField( "sort1", Type.STRING )
									)
								)
						)
						.orElseFail()
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				THIRD_ID, SECOND_ID, FIRST_ID, FOURTH_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_fromLuceneSortField_separateSort() {
		StubMappingScope scope = indexManager.createScope();

		SearchSort sort1 = scope.sort().extension()
						.ifSupported(
								LuceneExtension.get(),
								c2 -> c2.fromLuceneSortField( new SortField( "sort1", Type.STRING ) )
						)
						.orElseFail()
				.toSort();
		SearchSort sort2 = scope.sort().extension( LuceneExtension.get() )
				.fromLuceneSortField( new SortField( "sort2", Type.STRING ) )
				.toSort();
		SearchSort sort3 = scope.sort().extension()
				.ifSupported(
						LuceneExtension.get(),
						c2 -> c2.fromLuceneSortField( new SortField( "sort3", Type.STRING ) )
				)
				.orElseFail()
				.toSort();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.composite().add( sort1 ).add( sort2 ).add( sort3 ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID );

		SearchSort sort = scope.sort()
				.extension( LuceneExtension.get() ).fromLuceneSort( new Sort(
						new SortField( "sort3", Type.STRING ),
						new SortField( "sort2", Type.STRING ),
						new SortField( "sort1", Type.STRING )
					)
				)
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, FOURTH_ID, FIFTH_ID );
	}

	@Test
	public void predicate_nativeField() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"match() predicate on unsupported native field",
				() -> scope.query()
						.where( f -> f.match().field( "nativeField" ).matching( "37" ) )
						.toQuery()
				)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Native fields do not support defining predicates with the DSL: use the Lucene extension and a native query." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField" )
				) );
	}

	@Test
	public void predicate_nativeField_fromLuceneQuery() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.extension( LuceneExtension.get() )
						.fromLuceneQuery( new TermQuery( new Term( "nativeField", "37" ) ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID );
	}

	@Test
	public void predicate_nativeField_exists() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"exists() predicate on unsupported native field",
				() -> scope.predicate().exists().field( "nativeField" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Native fields do not support defining predicates with the DSL: use the Lucene extension and a native query." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField" )
				) );
	}

	@Test
	public void sort_nativeField() {
		StubMappingScope scope = indexManager.createScope();

		SubTest.expectException(
				"sort on unsupported native field",
				() -> scope.query()
						.where( f -> f.matchAll() )
						.sort( f -> f.field( "nativeField" ) )
						.toQuery()
				)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Native fields do not support defining sorts with the DSL: use the Lucene extension and a native sort." )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField" )
				) );
	}

	@Test
	public void sort_nativeField_fromLuceneSortField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.extension( LuceneExtension.get() ).fromLuceneSortField( new SortField( "nativeField", Type.LONG ) ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIFTH_ID, THIRD_ID, FIRST_ID, SECOND_ID, FOURTH_ID );
	}

	@Test
	public void projection_nativeField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Integer> query = scope.query()
				.asProjection( f -> f.field( "nativeField", Integer.class ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( 37 );
	}

	@Test
	public void projection_nativeField_withProjectionConverters_enabled() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<ValueWrapper> query = scope.query()
				.asProjection( f -> f.field( "nativeField_converted", ValueWrapper.class ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( new ValueWrapper<>( 37 ) );
	}

	@Test
	public void projection_nativeField_withProjectionConverters_disabled() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Integer> query = scope.query()
				.asProjection( f -> f.field( "nativeField_converted", Integer.class, ValueConvert.NO ) )
				.where( f -> f.match().field( "string" ).matching( "text 1" ) )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( 37 );
	}

	@Test
	public void projection_nativeField_unsupportedProjection() {
		StubMappingScope scope = indexManager.createScope();

		// let's check that it's possible to query the field beforehand
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.extension( LuceneExtension.get() )
						.fromLuceneQuery( new TermQuery( new Term( "nativeField_unsupportedProjection", "37" ) ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID );

		// now, let's check that projecting on the field throws an exception
		SubTest.expectException(
				"projection on native field not supporting projections",
				() -> scope.projection().field( "nativeField_unsupportedProjection", Integer.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Projections are not enabled for field" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( "nativeField_unsupportedProjection" )
				) );
	}

	@Test
	public void projection_document() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Document> query = scope.query()
				.asProjection(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.where( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetchAll().getHits();
		Assertions.assertThat( result )
				.hasSize( 5 )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "text 1" )
								.hasField( "nativeField", "37" )
								.hasField( "nativeField_converted", "37" )
								.hasField( "nativeField_unsupportedProjection", "37" )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "integer", 2 )
								.hasField( "nativeField", "78" )
								.hasField( "nativeField_converted", "78" )
								.hasField( "nativeField_unsupportedProjection", "78" )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "nativeField", "13" )
								.hasField( "nativeField_converted", "13" )
								.hasField( "nativeField_unsupportedProjection", "13" )
								// Geo points are stored as two internal fields
								.hasInternalField( "geoPoint_latitude", 40.12 )
								.hasInternalField( "geoPoint_longitude", -71.34 )
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
								// Geo points are stored as two internal fields
								.hasInternalField( "geoPoint_latitude", 45.12 )
								.hasInternalField( "geoPoint_longitude", -75.34 )
								.andOnlyInternalFields()
				) );
	}

	/**
	 * Check that the projection on a document includes all fields,
	 * even if there is a field projection, which would usually trigger document filtering.
	 */
	@Test
	public void projection_documentAndField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.extension( LuceneExtension.get() ).document(),
								f.field( "string" )
						)
				)
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<Document> result = query.fetchAll().getHits().stream()
				.map( list -> (Document) list.get( 0 ) )
				.collect( Collectors.toList() );
		Assertions.assertThat( result )
				.hasSize( 1 )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "text 1" )
								.hasField( "nativeField", "37" )
								.hasField( "nativeField_converted", "37" )
								.hasField( "nativeField_unsupportedProjection", "37" )
								.andOnlyInternalFields()
				) );
	}

	@Test
	public void projection_explanation() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Explanation> query = scope.query()
				.asProjection( f -> f.extension( LuceneExtension.get() ).explanation() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<Explanation> result = query.fetchAll().getHits();
		Assertions.assertThat( result ).hasSize( 1 );
		Assertions.assertThat( result.get( 0 ) )
				.isInstanceOf( Explanation.class )
				.extracting( Object::toString ).asString()
				.contains( MetadataFields.idFieldName() );
	}

	@Test
	public void nativeField_invalidFieldPath() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();

		SubTest.expectException(
				"native field contributing field with invalid field path",
				() -> plan.add( referenceProvider( FIRST_ID ), document -> {
					document.addValue( indexMapping.nativeField_invalidFieldPath, 45 );
				} ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid field path; expected path 'nativeField_invalidFieldPath', got 'not the expected path'." );
	}

	@Test
	public void backend_unwrap() {
		Backend backend = integration.getBackend( BACKEND_NAME );
		Assertions.assertThat( backend.unwrap( LuceneBackend.class ) )
				.isNotNull();
	}

	@Test
	public void backend_unwrap_error_unknownType() {
		Backend backend = integration.getBackend( BACKEND_NAME );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Attempt to unwrap a Lucene backend to '" + String.class.getName() + "'" );
		thrown.expectMessage( "this backend can only be unwrapped to '" + LuceneBackend.class.getName() + "'" );

		backend.unwrap( String.class );
	}

	@Test
	public void indexManager_unwrap() {
		IndexManager indexManager = integration.getIndexManager( INDEX_NAME );
		Assertions.assertThat( indexManager.unwrap( LuceneIndexManager.class ) )
				.isNotNull();
	}

	@Test
	public void indexManager_unwrap_error_unknownType() {
		IndexManager indexManager = integration.getIndexManager( INDEX_NAME );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Attempt to unwrap a Lucene index manager to '" + String.class.getName() + "'" );
		thrown.expectMessage( "this index manager can only be unwrapped to '" + LuceneIndexManager.class.getName() + "'" );

		indexManager.unwrap( String.class );
	}

	private void initData() {
		indexDataSet( indexMapping, indexManager );

		// Use the same IDs and dataset for otherIndexMapping to trigger
		// a failure in explain() tests if index selection doesn't work correctly.
		indexDataSet( otherIndexMapping, otherIndexManager );

		// Check that all documents are searchable
		assertThat( indexManager.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasDocRefHitsAnyOrder(
						INDEX_NAME,
						FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
				);
		assertThat( otherIndexManager.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasDocRefHitsAnyOrder(
						OTHER_INDEX_NAME,
						FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID
				);
	}

	private static void indexDataSet(IndexMapping indexMapping, StubMappingIndexManager indexManager) {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( FIRST_ID ), document -> {
			document.addValue( indexMapping.string, "text 1" );

			document.addValue( indexMapping.nativeField, 37 );
			document.addValue( indexMapping.nativeField_converted, 37 );
			document.addValue( indexMapping.nativeField_unsupportedProjection, 37 );

			document.addValue( indexMapping.sort1, "a" );
			document.addValue( indexMapping.sort2, "z" );
			document.addValue( indexMapping.sort3, "z" );
		} );
		plan.add( referenceProvider( SECOND_ID ), document -> {
			document.addValue( indexMapping.integer, 2 );

			document.addValue( indexMapping.nativeField, 78 );
			document.addValue( indexMapping.nativeField_converted, 78 );
			document.addValue( indexMapping.nativeField_unsupportedProjection, 78 );

			document.addValue( indexMapping.sort1, "z" );
			document.addValue( indexMapping.sort2, "a" );
			document.addValue( indexMapping.sort3, "z" );
		} );
		plan.add( referenceProvider( THIRD_ID ), document -> {
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 40.12, -71.34 ) );

			document.addValue( indexMapping.nativeField, 13 );
			document.addValue( indexMapping.nativeField_converted, 13 );
			document.addValue( indexMapping.nativeField_unsupportedProjection, 13 );

			document.addValue( indexMapping.sort1, "z" );
			document.addValue( indexMapping.sort2, "z" );
			document.addValue( indexMapping.sort3, "a" );
		} );
		plan.add( referenceProvider( FOURTH_ID ), document -> {
			document.addValue( indexMapping.nativeField, 89 );
			document.addValue( indexMapping.nativeField_converted, 89 );
			document.addValue( indexMapping.nativeField_unsupportedProjection, 89 );

			document.addValue( indexMapping.sort1, "z" );
			document.addValue( indexMapping.sort2, "z" );
			document.addValue( indexMapping.sort3, "z" );
		} );
		plan.add( referenceProvider( FIFTH_ID ), document -> {
			// This document should not match any query
			document.addValue( indexMapping.string, "text 2" );
			document.addValue( indexMapping.integer, 1 );
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 45.12, -75.34 ) );

			document.addValue( indexMapping.sort1, "zz" );
			document.addValue( indexMapping.sort2, "zz" );
			document.addValue( indexMapping.sort3, "zz" );
		} );
		plan.execute().join();
	}

	private static class IndexMapping {
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

		IndexMapping(IndexSchemaElement root) {
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
							.asNative( Integer.class, LuceneExtensionIT::contributeNativeField, LuceneExtensionIT::fromNativeField )
			)
					.toReference();
			nativeField_converted = root.field(
					"nativeField_converted",
					f -> f.extension( LuceneExtension.get() )
							.asNative( Integer.class, LuceneExtensionIT::contributeNativeField, LuceneExtensionIT::fromNativeField )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromIndexFieldConverter() )
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
		}
	}

	private static void contributeNativeField(String absoluteFieldPath, Integer value, Consumer<IndexableField> collector) {
		collector.accept( new StringField( absoluteFieldPath, value.toString(), Store.YES ) );
		collector.accept( new NumericDocValuesField( absoluteFieldPath, value.longValue() ) );
	}

	private static Integer fromNativeField(IndexableField field) {
		return Integer.parseInt( field.stringValue() );
	}

	private static void contributeNativeFieldInvalidFieldPath(String absoluteFieldPath, Integer value, Consumer<IndexableField> collector) {
		collector.accept( new StringField( "not the expected path", value.toString(), Store.YES ) );
	}
}
