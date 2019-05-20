/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultDefinitionContext;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.http.nio.client.HttpAsyncClient;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.HamcrestCondition;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class ElasticsearchExtensionIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "IndexName";
	private static final String OTHER_INDEX_NAME = "OtherIndexName";

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String FOURTH_ID = "4";
	private static final String FIFTH_ID = "5";
	private static final String EMPTY_ID = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SearchIntegration integration;

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private StubMappingIndexManager otherIndexManager;

	@Before
	public void setup() {
		this.integration = setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						OTHER_INDEX_NAME,
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.otherIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	@SuppressWarnings("unused")
	public void queryContext() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// Put intermediary contexts into variables to check they have the right type
		ElasticsearchSearchQueryResultDefinitionContext<DocumentReference, DocumentReference> context1 =
				scope.query().extension( ElasticsearchExtension.get() );
		ElasticsearchSearchQueryResultContext<DocumentReference> context2 = context1.asProjection(
				f -> f.composite(
						// We don't care about the source, it's just to test that the factory context allows ES-specific projection
						(docRef, source) -> docRef,
						f.documentReference(), f.source()
				)
		);
		// Note we can use Elasticsearch-specific predicates immediately
		ElasticsearchSearchQueryContext<DocumentReference> context3 =
				context2.predicate( f -> f.fromJson( "{'match_all': {}}" ) );
		// Note we can use Elasticsearch-specific sorts immediately
		ElasticsearchSearchQueryContext<DocumentReference> context4 =
				context3.sort( c -> c.fromJson( "{'sort1': 'asc'}" ) );

		// Put the query and result into variables to check they have the right type
		ElasticsearchSearchQuery<DocumentReference> query = context4.toQuery();
		ElasticsearchSearchResult<DocumentReference> result = query.fetch();

		assertThat( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID )
				.hasTotalHitCount( 6 );

		// Also check (at compile time) the context type for other asXXX() methods, since we need to override each method explicitly
		ElasticsearchSearchQueryResultContext<DocumentReference> asReferenceContext =
				scope.query().extension( ElasticsearchExtension.get() ).asReference();
		ElasticsearchSearchQueryResultContext<DocumentReference> asEntityContext =
				scope.query().extension( ElasticsearchExtension.get() ).asEntity();
		SearchProjection<DocumentReference> projection = scope.projection().documentReference().toProjection();
		ElasticsearchSearchQueryResultContext<DocumentReference> asProjectionContext =
				scope.query().extension( ElasticsearchExtension.get() ).asProjection( projection );
		ElasticsearchSearchQueryResultContext<List<?>> asProjectionsContext =
				scope.query().extension( ElasticsearchExtension.get() ).asProjections( projection, projection );
		ElasticsearchSearchQueryContext<DocumentReference> defaultResultContext =
				scope.query().extension( ElasticsearchExtension.get() )
						.predicate( f -> f.fromJson( "{'match_all': {}}" ) );
	}

	@Test
	public void query() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<DocumentReference> genericQuery = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();

		// Put the query and result into variables to check they have the right type
		ElasticsearchSearchQuery<DocumentReference> query = genericQuery.extension( ElasticsearchExtension.get() );
		ElasticsearchSearchResult<DocumentReference> result = query.fetch();
		assertThat( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID )
				.hasTotalHitCount( 6 );

		// Unsupported extension
		SubTest.expectException(
				() -> query.extension( new SearchQueryExtension<Void, DocumentReference>() {
					@Override
					public Optional<Void> extendOptional(SearchQuery<DocumentReference> original,
							LoadingContext<?, ?> loadingContext) {
						return Optional.empty();
					}
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void query_explain_singleIndex() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.predicate( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		Assertions.assertThat( query.explain( FIRST_ID ) )
				.contains( "\"description\":" )
				.contains( "\"details\":" );

		// Non-matching document
		Assertions.assertThat( query.explain( FIFTH_ID ) )
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void query_explain_singleIndex_invalidId() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.predicate( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Non-existing document
		SubTest.expectException(
				() -> query.explain( "InvalidId" )
		)
				.assertThrown()
				.has( new HamcrestCondition<>(
						ExceptionMatcherBuilder.isException( SearchException.class )
								.causedBy( SearchException.class )
								.withMessage(
										"Document with id 'InvalidId' does not exist in index '"
												+ ElasticsearchIndexNameNormalizer.normalize( INDEX_NAME ) + "'"
								)
								.withMessage( "its match cannot be explained" )
								.build()
				) );
	}

	@Test
	public void query_explain_multipleIndexes() {
		StubMappingSearchScope scope = indexManager.createSearchScope( otherIndexManager );

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.predicate( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		Assertions.assertThat( query.explain( INDEX_NAME, FIRST_ID ) )
				.contains( "\"description\":" )
				.contains( "\"details\":" );

		// Non-matching document
		Assertions.assertThat( query.explain( INDEX_NAME, FIFTH_ID ) )
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void query_explain_multipleIndexes_missingIndexName() {
		StubMappingSearchScope scope = indexManager.createSearchScope( otherIndexManager );

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.predicate( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		SubTest.expectException(
				() -> query.explain( FIRST_ID )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "explain(String id) cannot be used when the query targets multiple indexes" )
				.hasMessageContaining(
						"pass one of [" +
						ElasticsearchIndexNameNormalizer.normalize( INDEX_NAME )
						+ ", " + ElasticsearchIndexNameNormalizer.normalize( OTHER_INDEX_NAME )
						+ "]"
				);
	}

	@Test
	public void query_explain_multipleIndexes_invalidIndexName() {
		StubMappingSearchScope scope = indexManager.createSearchScope( otherIndexManager );

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.predicate( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		SubTest.expectException(
				() -> query.explain( "NotAnIndexName", FIRST_ID )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"index name 'notanindexname' is not among the indexes targeted by this query: ["
						+ ElasticsearchIndexNameNormalizer.normalize( INDEX_NAME )
						+ ", " + ElasticsearchIndexNameNormalizer.normalize( OTHER_INDEX_NAME )
						+ "]"
				);
	}

	@Test
	public void predicate_fromJson() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool()
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson( "{'match': {'string': 'text 1'}}" )
						)
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson( "{'match': {'integer': 2}}" )
						)
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson(
										"{"
											+ "'geo_distance': {"
												+ "'distance': '200km',"
												+ "'geoPoint': {"
													+ "'lat': 40,"
													+ "'lon': -70"
												+ "}"
											+ "}"
										+ "}"
								)
						)
						// Also test using the standard DSL on a field defined with the extension
						.should( f.match().onField( "dateWithColons" ).matching( "'2018:01:12'" ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID )
				.hasTotalHitCount( 4 );
	}

	@Test
	public void predicate_fromJson_separatePredicate() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchPredicate predicate1 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( "{'match': {'string': 'text 1'}}" ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( "{'match': {'integer': 2}}" ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson(
						"{"
							+ "'geo_distance': {"
								+ "'distance': '200km',"
								+ "'geoPoint': {"
									+ "'lat': 40,"
									+ "'lon': -70"
								+ "}"
							+ "}"
						+ "}"
				)
				.toPredicate();
		// Also test using the standard DSL on a field defined with the extension
		SearchPredicate predicate4 = scope.predicate().match().onField( "dateWithColons" )
				.matching( "'2018:01:12'" ).toPredicate();
		SearchPredicate booleanPredicate = scope.predicate().bool()
				.should( predicate1 )
				.should( predicate2 )
				.should( predicate3 )
				.should( predicate4 )
				.toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( booleanPredicate )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID )
				.hasTotalHitCount( 4 );
	}

	@Test
	public void sort_fromJson() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c
						.extension( ElasticsearchExtension.get() )
								.fromJson( "{'sort1': 'asc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'sort2': 'asc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'sort3': 'asc'}" )
						// Also test using the standard DSL on a field defined with the extension
						.then().byField( "sort4" ).asc().onMissingValue().sortLast()
						.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID
		);

		query = scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c
						.extension( ElasticsearchExtension.get() )
								.fromJson( "{'sort1': 'desc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'sort2': 'desc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'sort3': 'desc'}" )
						.then().byField( "sort4" ).desc().onMissingValue().sortLast()
						.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_fromJson_separateSort() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchSort sort1Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'sort1': 'asc'}" )
				.toSort();
		SearchSort sort2Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'sort2': 'asc'}" )
				.toSort();
		SearchSort sort3Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'sort3': 'asc'}" )
				.toSort();
		// Also test using the standard DSL on a field defined with the extension
		SearchSort sort4Asc = scope.sort()
				.byField( "sort4" ).asc().onMissingValue().sortLast()
				.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				.toSort();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sort1Asc ).then().by( sort2Asc ).then().by( sort3Asc ).then().by( sort4Asc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID );

		SearchSort sort1Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'sort1': 'desc'}" )
				.toSort();
		SearchSort sort2Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'sort2': 'desc'}" )
				.toSort();
		SearchSort sort3Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'sort3': 'desc'}" )
				.toSort();
		SearchSort sort4Desc = scope.sort()
				.byField( "sort4" ).desc().onMissingValue().sortLast()
				.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				.toSort();

		query = scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sort1Desc ).then().by( sort2Desc ).then().by( sort3Desc ).then().by( sort4Desc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID );
	}

	@Test
	public void projection_document() throws JSONException {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<String> query = scope.query()
				.asProjection(
						f -> f.extension( ElasticsearchExtension.get() ).source()
				)
				.predicate( f -> f.id().matching( FIFTH_ID ) )
				.toQuery();

		List<String> result = query.fetch().getHits();
		Assertions.assertThat( result ).hasSize( 1 );
		JSONAssert.assertEquals(
				"{"
						+ "'string': 'text 2',"
						+ "'integer': 1,"
						+ "'geoPoint': {'lat': 45.12, 'lon': -75.34},"
						+ "'dateWithColons': '2018:01:25',"
						+ "'sort5': 'z'"
						+ "}",
				result.get( 0 ),
				JSONCompareMode.STRICT
		);
	}

	/**
	 * Check that the projection on source includes all fields,
	 * even if there is a field projection, which would usually trigger source filtering.
	 */
	@Test
	public void projection_documentAndField() throws JSONException {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.extension( ElasticsearchExtension.get() ).source(),
								f.field( "string" )
						)
				)
				.predicate( f -> f.id().matching( FIFTH_ID ) )
				.toQuery();

		List<String> result = query.fetch().getHits().stream()
				.map( list -> (String) list.get( 0 ) )
				.collect( Collectors.toList() );
		Assertions.assertThat( result ).hasSize( 1 );
		JSONAssert.assertEquals(
				"{"
						+ "'string': 'text 2',"
						+ "'integer': 1,"
						+ "'geoPoint': {'lat': 45.12, 'lon': -75.34},"
						+ "'dateWithColons': '2018:01:25',"
						+ "'sort5': 'z'"
						+ "}",
				result.get( 0 ),
				JSONCompareMode.STRICT
		);
	}

	@Test
	public void projection_explanation() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<String> query = scope.query()
				.asProjection( f -> f.extension( ElasticsearchExtension.get() ).explanation() )
				.predicate( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<String> result = query.fetch().getHits();
		Assertions.assertThat( result ).hasSize( 1 );
		Assertions.assertThat( result.get( 0 ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void backend_unwrap() {
		Backend backend = integration.getBackend( BACKEND_NAME );
		Assertions.assertThat( backend.unwrap( ElasticsearchBackend.class ) )
				.isNotNull();
	}

	@Test
	public void backend_unwrap_error_unknownType() {
		Backend backend = integration.getBackend( BACKEND_NAME );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Attempt to unwrap an Elasticsearch backend to '" + String.class.getName() + "'" );
		thrown.expectMessage( "this backend can only be unwrapped to '" + ElasticsearchBackend.class.getName() + "'" );

		backend.unwrap( String.class );
	}

	@Test
	public void backend_getClient() throws Exception {
		Backend backend = integration.getBackend( BACKEND_NAME );
		ElasticsearchBackend elasticsearchBackend = backend.unwrap( ElasticsearchBackend.class );
		RestClient restClient = elasticsearchBackend.getClient( RestClient.class );

		// Test that the client actually works
		Response response = restClient.performRequest( new Request( "GET", "/" ) );
		Assertions.assertThat( response.getStatusLine().getStatusCode() ).isEqualTo( 200 );
	}

	@Test
	public void backend_getClient_error_invalidClass() {
		Backend backend = integration.getBackend( BACKEND_NAME );
		ElasticsearchBackend elasticsearchBackend = backend.unwrap( ElasticsearchBackend.class );

		thrown.expect( SearchException.class );
		thrown.expectMessage( HttpAsyncClient.class.getName() );
		thrown.expectMessage( "the client can only be unwrapped to" );
		thrown.expectMessage( RestClient.class.getName() );

		elasticsearchBackend.getClient( HttpAsyncClient.class );
	}

	@Test
	public void indexManager_unwrap() {
		IndexManager indexManager = integration.getIndexManager( INDEX_NAME );
		Assertions.assertThat( indexManager.unwrap( ElasticsearchIndexManager.class ) )
				.isNotNull();
	}

	@Test
	public void indexManager_unwrap_error_unknownType() {
		IndexManager indexManager = integration.getIndexManager( INDEX_NAME );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Attempt to unwrap an Elasticsearch index manager to '" + String.class.getName() + "'" );
		thrown.expectMessage( "this index manager can only be unwrapped to '" + ElasticsearchIndexManager.class.getName() + "'" );

		indexManager.unwrap( String.class );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( SECOND_ID ), document -> {
			document.addValue( indexMapping.integer, "2" );

			document.addValue( indexMapping.sort1, "z" );
			document.addValue( indexMapping.sort2, "a" );
			document.addValue( indexMapping.sort3, "z" );
			document.addValue( indexMapping.sort4, "z" );
			document.addValue( indexMapping.sort5, "a" );
		} );
		workPlan.add( referenceProvider( FIRST_ID ), document -> {
			document.addValue( indexMapping.string, "'text 1'" );

			document.addValue( indexMapping.sort1, "a" );
			document.addValue( indexMapping.sort2, "z" );
			document.addValue( indexMapping.sort3, "z" );
			document.addValue( indexMapping.sort4, "z" );
			document.addValue( indexMapping.sort5, "a" );
		} );
		workPlan.add( referenceProvider( THIRD_ID ), document -> {
			document.addValue( indexMapping.geoPoint, "{'lat': 40.12, 'lon': -71.34}" );

			document.addValue( indexMapping.sort1, "z" );
			document.addValue( indexMapping.sort2, "z" );
			document.addValue( indexMapping.sort3, "a" );
			document.addValue( indexMapping.sort4, "z" );
			document.addValue( indexMapping.sort5, "a" );
		} );
		workPlan.add( referenceProvider( FOURTH_ID ), document -> {
			document.addValue( indexMapping.dateWithColons, "'2018:01:12'" );

			document.addValue( indexMapping.sort1, "z" );
			document.addValue( indexMapping.sort2, "z" );
			document.addValue( indexMapping.sort3, "z" );
			document.addValue( indexMapping.sort4, "a" );
			document.addValue( indexMapping.sort5, "a" );
		} );
		workPlan.add( referenceProvider( FIFTH_ID ), document -> {
			// This document should not match any query
			document.addValue( indexMapping.string, "'text 2'" );
			document.addValue( indexMapping.integer, "1" );
			document.addValue( indexMapping.geoPoint, "{'lat': 45.12, 'lon': -75.34}" );
			document.addValue( indexMapping.dateWithColons, "'2018:01:25'" );

			document.addValue( indexMapping.sort5, "z" );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID
		);
	}

	private static class IndexMapping {
		final IndexFieldReference<String> integer;
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> geoPoint;
		final IndexFieldReference<String> dateWithColons;

		final IndexFieldReference<String> sort1;
		final IndexFieldReference<String> sort2;
		final IndexFieldReference<String> sort3;
		final IndexFieldReference<String> sort4;
		final IndexFieldReference<String> sort5;

		IndexMapping(IndexSchemaElement root) {
			integer = root.field(
					"integer",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'integer'}" )
			)
					.toReference();
			string = root.field(
					"string",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'keyword'}" )
			)
					.toReference();
			geoPoint = root.field(
					"geoPoint",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'geo_point'}" )
			)
					.toReference();
			dateWithColons = root.field(
					"dateWithColons",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'date', 'format': 'yyyy:MM:dd'}" )
			)
					.toReference();

			sort1 = root.field(
					"sort1",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
			sort2 = root.field(
					"sort2",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
			sort3 = root.field(
					"sort3",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
			sort4 = root.field(
					"sort4",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
			sort5 = root.field(
					"sort5",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
		}
	}

}