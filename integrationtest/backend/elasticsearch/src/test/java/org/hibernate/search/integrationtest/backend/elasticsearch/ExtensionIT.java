/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchSort;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.http.nio.client.HttpAsyncClient;
import org.assertj.core.api.Assertions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

public class ExtensionIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";

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

	private SearchMappingRepository mappingRepository;
	private IndexAccessors indexAccessors;
	private IndexManager<?> indexManager;
	private String indexName;
	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		this.mappingRepository = setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndex(
						"MappedType", "IndexName",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						(indexManager, indexName) -> {
							this.indexManager = indexManager;
							this.indexName = indexName;
						}
				)
				.setup();

		initData();
	}

	@Test
	public void predicate_fromJsonString() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().bool( b -> {
					b.should().withExtensionOptional(
							ElasticsearchExtension.get(),
							// FIXME find some way to forbid using the context twice... ?
							c -> c.fromJsonString( "{'match': {'string': 'text 1'}}" )
					);
					b.should().withExtension( ElasticsearchExtension.get() )
							.fromJsonString( "{'match': {'integer': 2}}" );
					b.should().withExtensionOptional(
							ElasticsearchExtension.get(),
							// FIXME find some way to forbid using the context twice... ?
							c -> c.fromJsonString(
									"{"
										+ "'geo_distance': {"
											+ "'distance': '200km',"
											+ "'geoPoint': {"
												+ "'lat': 40,"
												+ "'lon': -70"
											+ "}"
										+ "}"
									+ "}" ),
							c -> Assert.fail( "Expected the extension to be present" )
					);
					// Also test using the standard DSL on a field defined with the extension
					b.should().match().onField( "yearDays" ).matching( "'2018:12'" );
				} )
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID )
				.hasHitCount( 4 );
	}

	@Test
	public void predicate_fromJsonString_separatePredicate() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchPredicate predicate1 = searchTarget.predicate().withExtensionOptional(
				ElasticsearchExtension.get(),
				// FIXME find some way to forbid using the context twice... ?
				c -> c.fromJsonString( "{'match': {'string': 'text 1'}}" )
		);
		SearchPredicate predicate2 = searchTarget.predicate().withExtension( ElasticsearchExtension.get() )
				.fromJsonString( "{'match': {'integer': 2}}" );
		SearchPredicate predicate3 = searchTarget.predicate().withExtensionOptional(
				ElasticsearchExtension.get(),
				// FIXME find some way to forbid using the context twice... ?
				c -> c.fromJsonString(
						"{"
							+ "'geo_distance': {"
								+ "'distance': '200km',"
								+ "'geoPoint': {"
									+ "'lat': 40,"
									+ "'lon': -70"
								+ "}"
							+ "}"
						+ "}" ),
				c -> Assert.fail( "Expected the extension to be present" )
		);
		// Also test using the standard DSL on a field defined with the extension
		SearchPredicate predicate4 = searchTarget.predicate().match().onField( "yearDays" )
				.matching( "'2018:12'" );
		SearchPredicate booleanPredicate = searchTarget.predicate().bool( b -> {
			b.should( predicate1 );
			b.should( predicate2 );
			b.should( predicate3 );
			b.should( predicate4 );
		} );

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( booleanPredicate )
				.build();
		assertThat( query )
				.hasReferencesHitsAnyOrder( indexName, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID )
				.hasHitCount( 4 );
	}

	@Test
	public void sort_fromJsonString() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().all().end()
				.sort( c -> c
						.withExtensionOptional(
								ElasticsearchExtension.get(),
								c2 -> c2.fromJsonString( "{'sort1': 'asc'}" )
						)
						.then().withExtension( ElasticsearchExtension.get() )
								.fromJsonString( "{'sort2': 'asc'}" )
						.then().withExtensionOptional(
								ElasticsearchExtension.get(),
								c2 -> c2.fromJsonString( "{'sort3': 'asc'}" ),
								c2 -> Assert.fail( "Expected the extension to be present" )
						)
						// Also test using the standard DSL on a field defined with the extension
						.then().byField( "sort4" ).asc().onMissingValue().sortLast()
						.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				)
				.build();
		assertThat( query ).hasReferencesHitsExactOrder(
				indexName,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID
		);

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().all().end()
				.sort( c -> c
						.withExtensionOptional(
								ElasticsearchExtension.get(),
								c2 -> c2.fromJsonString( "{'sort1': 'desc'}" )
						)
						.then().withExtension( ElasticsearchExtension.get() )
								.fromJsonString( "{'sort2': 'desc'}" )
						.then().withExtensionOptional(
								ElasticsearchExtension.get(),
								c2 -> c2.fromJsonString( "{'sort3': 'desc'}" ),
								c2 -> Assert.fail( "Expected the extension to be present" )
						)
						.then().byField( "sort4" ).desc().onMissingValue().sortLast()
						.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				)
				.build();
		assertThat( query ).hasReferencesHitsExactOrder(
				indexName,
				FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_fromJsonString_separateSort() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchSort sort1 = searchTarget.sort()
				.withExtensionOptional(
						ElasticsearchExtension.get(),
						c2 -> c2.fromJsonString( "{'sort1': 'asc'}" )
				)
				.end();
		SearchSort sort2 = searchTarget.sort().withExtension( ElasticsearchExtension.get() )
				.fromJsonString( "{'sort2': 'asc'}" )
				.end();
		SearchSort sort3 = searchTarget.sort()
				.withExtensionOptional(
						ElasticsearchExtension.get(),
						c2 -> c2.fromJsonString( "{'sort3': 'asc'}" ),
						c2 -> Assert.fail( "Expected the extension to be present" )
				)
				.end();
		// Also test using the standard DSL on a field defined with the extension
		SearchSort sort4 = searchTarget.sort()
				.byField( "sort4" ).asc().onMissingValue().sortLast()
				.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				.end();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().all().end()
				.sort().by( sort1 ).then().by( sort2 ).then().by( sort3 ).then().by( sort4 ).end()
				.build();
		assertThat( query )
				.hasReferencesHitsExactOrder( indexName, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID );

		sort1 = searchTarget.sort()
				.withExtensionOptional(
						ElasticsearchExtension.get(),
						c2 -> c2.fromJsonString( "{'sort1': 'desc'}" )
				)
				.end();
		sort2 = searchTarget.sort().withExtension( ElasticsearchExtension.get() )
				.fromJsonString( "{'sort2': 'desc'}" )
				.end();
		sort3 = searchTarget.sort()
				.withExtensionOptional(
						ElasticsearchExtension.get(),
						c2 -> c2.fromJsonString( "{'sort3': 'desc'}" ),
						c2 -> Assert.fail( "Expected the extension to be present" )
				)
				.end();
		sort4 = searchTarget.sort()
				.byField( "sort4" ).desc().onMissingValue().sortLast()
				.then().byField( "sort5" ).asc().onMissingValue().sortFirst()
				.end();

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().all().end()
				.sort().by( sort1 ).then().by( sort2 ).then().by( sort3 ).then().by( sort4 ).end()
				.build();
		assertThat( query )
				.hasReferencesHitsExactOrder( indexName, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID );
	}

	@Test
	public void backend_getClient() throws Exception {
		Backend backend = mappingRepository.getBackend( BACKEND_NAME );
		ElasticsearchBackend elasticsearchBackend = backend.withExtension( ElasticsearchExtension.get() );
		RestClient restClient = elasticsearchBackend.getClient( RestClient.class );

		// Test that the client actually works
		Response response = restClient.performRequest( "GET", "/" );
		Assertions.assertThat( response.getStatusLine().getStatusCode() ).isEqualTo( 200 );
	}

	@Test
	public void backend_getClient_error_invalidClass() {
		Backend backend = mappingRepository.getBackend( BACKEND_NAME );
		ElasticsearchBackend elasticsearchBackend = backend.withExtension( ElasticsearchExtension.get() );

		thrown.expect( SearchException.class );
		thrown.expectMessage( HttpAsyncClient.class.getName() );
		thrown.expectMessage( "the client can only be unwrapped to" );
		thrown.expectMessage( RestClient.class.getName() );

		elasticsearchBackend.getClient( HttpAsyncClient.class );
	}

	private void initData() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( sessionContext );
		worker.add( referenceProvider( SECOND_ID ), document -> {
			indexAccessors.integer.write( document, "2" );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "a" );
			indexAccessors.sort3.write( document, "z" );
			indexAccessors.sort4.write( document, "z" );
			indexAccessors.sort5.write( document, "a" );
		} );
		worker.add( referenceProvider( FIRST_ID ), document -> {
			indexAccessors.string.write( document, "'text 1'" );

			indexAccessors.sort1.write( document, "a" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "z" );
			indexAccessors.sort4.write( document, "z" );
			indexAccessors.sort5.write( document, "a" );
		} );
		worker.add( referenceProvider( THIRD_ID ), document -> {
			indexAccessors.geoPoint.write( document, "{'lat': 40.12, 'lon': -71.34}" );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "a" );
			indexAccessors.sort4.write( document, "z" );
			indexAccessors.sort5.write( document, "a" );
		} );
		worker.add( referenceProvider( FOURTH_ID ), document -> {
			indexAccessors.yearDays.write( document, "'2018:012'" );

			indexAccessors.sort1.write( document, "z" );
			indexAccessors.sort2.write( document, "z" );
			indexAccessors.sort3.write( document, "z" );
			indexAccessors.sort4.write( document, "a" );
			indexAccessors.sort5.write( document, "a" );
		} );
		worker.add( referenceProvider( FIFTH_ID ), document -> {
			// This document should not match any query
			indexAccessors.string.write( document, "'text 2'" );
			indexAccessors.integer.write( document, "1" );
			indexAccessors.geoPoint.write( document, "{'lat': 45.12, 'lon': -75.34}" );
			indexAccessors.yearDays.write( document, "'2018:025'" );

			indexAccessors.sort5.write( document, "z" );
		} );
		worker.add( referenceProvider( EMPTY_ID ), document -> { } );

		worker.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder(
				indexName,
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
			integer = root.field( "integer" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'integer'}" )
					.createAccessor();
			string = root.field( "string" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'keyword'}" )
					.createAccessor();
			geoPoint = root.field( "geoPoint" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'geo_point'}" )
					.createAccessor();
			yearDays = root.field( "yearDays" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'date', 'format': 'yyyy:DDD'}" )
					.createAccessor();

			sort1 = root.field( "sort1" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
					.createAccessor();
			sort2 = root.field( "sort2" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
					.createAccessor();
			sort3 = root.field( "sort3" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
					.createAccessor();
			sort4 = root.field( "sort4" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
					.createAccessor();
			sort5 = root.field( "sort5" )
					.withExtension( ElasticsearchExtension.get() )
					.asJsonString( "{'type': 'keyword', 'doc_values': true}" )
					.createAccessor();
		}
	}

}