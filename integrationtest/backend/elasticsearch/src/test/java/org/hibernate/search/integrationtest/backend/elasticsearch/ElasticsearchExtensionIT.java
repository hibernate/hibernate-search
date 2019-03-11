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
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.http.nio.client.HttpAsyncClient;
import org.assertj.core.api.Assertions;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class ElasticsearchExtensionIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "IndexName";

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
	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		this.integration = setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void predicate_fromJsonString() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJsonString( "{'match': {'string': 'text 1'}}" )
						)
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJsonString( "{'match': {'integer': 2}}" )
						)
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJsonString(
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
						.should( f.match().onField( "yearDays" ).matching( "'2018:12'" ) )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID )
				.hasHitCount( 4 );
	}

	@Test
	public void predicate_fromJsonString_separatePredicate() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchPredicate predicate1 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJsonString( "{'match': {'string': 'text 1'}}" ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJsonString( "{'match': {'integer': 2}}" ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJsonString(
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
		SearchPredicate predicate4 = scope.predicate().match().onField( "yearDays" )
				.matching( "'2018:12'" ).toPredicate();
		SearchPredicate booleanPredicate = scope.predicate().bool()
				.should( predicate1 )
				.should( predicate2 )
				.should( predicate3 )
				.should( predicate4 )
				.toPredicate();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( booleanPredicate )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID )
				.hasHitCount( 4 );
	}

	@Test
	public void sort_fromJsonString() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c
						.extension( ElasticsearchExtension.get() )
								.fromJsonString( "{'sort1': 'asc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJsonString( "{'sort2': 'asc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJsonString( "{'sort3': 'asc'}" )
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
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c
						.extension( ElasticsearchExtension.get() )
								.fromJsonString( "{'sort1': 'desc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJsonString( "{'sort2': 'desc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJsonString( "{'sort3': 'desc'}" )
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
	public void sort_fromJsonString_separateSort() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchSort sort1Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJsonString( "{'sort1': 'asc'}" )
				.toSort();
		SearchSort sort2Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJsonString( "{'sort2': 'asc'}" )
				.toSort();
		SearchSort sort3Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJsonString( "{'sort3': 'asc'}" )
				.toSort();
		// Also test using the standard DSL on a field defined with the extension
		SearchSort sort4Asc = scope.sort()
				.byField( "sort4" ).asc().onMissingValue().sortLast()
				.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				.toSort();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sort1Asc ).then().by( sort2Asc ).then().by( sort3Asc ).then().by( sort4Asc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID );

		SearchSort sort1Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJsonString( "{'sort1': 'desc'}" )
				.toSort();
		SearchSort sort2Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJsonString( "{'sort2': 'desc'}" )
				.toSort();
		SearchSort sort3Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJsonString( "{'sort3': 'desc'}" )
				.toSort();
		SearchSort sort4Desc = scope.sort()
				.byField( "sort4" ).desc().onMissingValue().sortLast()
				.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				.toSort();

		query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sort1Desc ).then().by( sort2Desc ).then().by( sort3Desc ).then().by( sort4Desc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID );
	}

	@Test
	public void projection_document() throws JSONException {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<String> query = scope.query()
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
						+ "'yearDays': '2018:025',"
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

		IndexSearchQuery<List<?>> query = scope.query()
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
						+ "'yearDays': '2018:025',"
						+ "'sort5': 'z'"
						+ "}",
				result.get( 0 ),
				JSONCompareMode.STRICT
		);
	}

	@Test
	public void projection_explanation() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<String> query = scope.query()
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
			indexAccessors.integer.write( document, "2" );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "a" );
			indexAccessors.sort3.write( document, "z" );
			indexAccessors.sort4.write( document, "z" );
			indexAccessors.sort5.write( document, "a" );
		} );
		workPlan.add( referenceProvider( FIRST_ID ), document -> {
			indexAccessors.string.write( document, "'text 1'" );

			indexAccessors.sort1.write( document, "a" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "z" );
			indexAccessors.sort4.write( document, "z" );
			indexAccessors.sort5.write( document, "a" );
		} );
		workPlan.add( referenceProvider( THIRD_ID ), document -> {
			indexAccessors.geoPoint.write( document, "{'lat': 40.12, 'lon': -71.34}" );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "a" );
			indexAccessors.sort4.write( document, "z" );
			indexAccessors.sort5.write( document, "a" );
		} );
		workPlan.add( referenceProvider( FOURTH_ID ), document -> {
			indexAccessors.yearDays.write( document, "'2018:012'" );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "z" );
			indexAccessors.sort4.write( document, "a" );
			indexAccessors.sort5.write( document, "a" );
		} );
		workPlan.add( referenceProvider( FIFTH_ID ), document -> {
			// This document should not match any query
			indexAccessors.string.write( document, "'text 2'" );
			indexAccessors.integer.write( document, "1" );
			indexAccessors.geoPoint.write( document, "{'lat': 45.12, 'lon': -75.34}" );
			indexAccessors.yearDays.write( document, "'2018:025'" );

			indexAccessors.sort5.write( document, "z" );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID
		);
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> integer;
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<String> geoPoint;
		final IndexFieldAccessor<String> yearDays;

		final IndexFieldAccessor<String> sort1;
		final IndexFieldAccessor<String> sort2;
		final IndexFieldAccessor<String> sort3;
		final IndexFieldAccessor<String> sort4;
		final IndexFieldAccessor<String> sort5;

		IndexAccessors(IndexSchemaElement root) {
			integer = root.field(
					"integer",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'integer'}" )
			)
					.createAccessor();
			string = root.field(
					"string",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'keyword'}" )
			)
					.createAccessor();
			geoPoint = root.field(
					"geoPoint",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'geo_point'}" )
			)
					.createAccessor();
			yearDays = root.field(
					"yearDays",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'date', 'format': 'yyyy:DDD'}" )
			)
					.createAccessor();

			sort1 = root.field(
					"sort1",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
			)
					.createAccessor();
			sort2 = root.field(
					"sort2",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
			)
					.createAccessor();
			sort3 = root.field(
					"sort3",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
			)
					.createAccessor();
			sort4 = root.field(
					"sort4",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
			)
					.createAccessor();
			sort5 = root.field(
					"sort5",
					f -> f.extension( ElasticsearchExtension.get() )
							.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
			)
					.createAccessor();
		}
	}

}