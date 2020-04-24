/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryOptionsStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryWhereStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQuerySelectStep;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.http.nio.client.HttpAsyncClient;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.HamcrestCondition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONException;
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

	private SearchIntegration integration;

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private StubMappingIndexManager otherIndexManager;

	private Gson gson = new Gson();

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
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
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
		ElasticsearchSearchQuerySelectStep<DocumentReference, DocumentReference, StubLoadingOptionsStep> context1 =
				scope.query().extension( ElasticsearchExtension.get() );
		ElasticsearchSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> context2 = context1.select(
				f -> f.composite(
						// We don't care about the source, it's just to test that the factory context allows ES-specific projection
						(docRef, source) -> docRef,
						f.documentReference(), f.source()
				)
		);
		// Note we can use Elasticsearch-specific predicates immediately
		ElasticsearchSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> context3 =
				context2.where( f -> f.fromJson( "{'match_all': {}}" ) );
		// Note we can use Elasticsearch-specific sorts immediately
		ElasticsearchSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> context4 =
				context3.sort( f -> f.fromJson( "{'nativeField_sort1': 'asc'}" ) );

		// Put the query and result into variables to check they have the right type
		ElasticsearchSearchQuery<DocumentReference> query = context4.toQuery();
		ElasticsearchSearchResult<DocumentReference> result = query.fetchAll();

		assertThat( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID )
				.hasTotalHitCount( 6 );

		// Also check (at compile time) the context type for other asXXX() methods, since we need to override each method explicitly
		ElasticsearchSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> selectEntityReferenceContext =
				scope.query().extension( ElasticsearchExtension.get() ).selectEntityReference();
		ElasticsearchSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> selectEntityContext =
				scope.query().extension( ElasticsearchExtension.get() ).selectEntity();
		SearchProjection<DocumentReference> projection = scope.projection().documentReference().toProjection();
		ElasticsearchSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> selectProjectionContext =
				scope.query().extension( ElasticsearchExtension.get() ).select( projection );
		ElasticsearchSearchQueryWhereStep<List<?>, StubLoadingOptionsStep> selectProjectionsContext =
				scope.query().extension( ElasticsearchExtension.get() ).select( projection, projection );
		ElasticsearchSearchQueryOptionsStep<DocumentReference, StubLoadingOptionsStep> defaultResultContext =
				scope.query().extension( ElasticsearchExtension.get() )
						.where( f -> f.fromJson( "{'match_all': {}}" ) );
	}

	@Test
	public void query() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> genericQuery = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Put the query and result into variables to check they have the right type
		ElasticsearchSearchQuery<DocumentReference> query = genericQuery.extension( ElasticsearchExtension.get() );
		ElasticsearchSearchResult<DocumentReference> result = query.fetchAll();
		assertThat( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID )
				.hasTotalHitCount( 6 );

		// Unsupported extension
		Assertions.assertThatThrownBy(
				() -> query.extension( (SearchQuery<DocumentReference> original, LoadingContext<?, ?> loadingContext) -> Optional.empty() )
		)
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void query_gsonResponseBody() {
		StubMappingScope scope = indexManager.createScope();

		ElasticsearchSearchResult<DocumentReference> result = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.fetchAll();

		Assertions.assertThat( result.getResponseBody() )
				.isNotNull()
				.extracting( body -> body.get( "_shards" ) ).isInstanceOf( JsonObject.class );
	}

	@Test
	public void query_explain_singleIndex() {
		StubMappingScope scope = indexManager.createScope();

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		Assertions.assertThat( query.explain( FIRST_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );

		// Non-matching document
		Assertions.assertThat( query.explain( FIFTH_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3783")
	public void query_explain_projection() {
		StubMappingScope scope = indexManager.createScope();

		ElasticsearchSearchQuery<String> query = scope.query().extension( ElasticsearchExtension.get() )
				.select( f -> f.field( "string", String.class ) )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		Assertions.assertThat( query.explain( FIRST_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );

		// Non-matching document
		Assertions.assertThat( query.explain( FIFTH_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void query_explain_singleIndex_invalidId() {
		StubMappingScope scope = indexManager.createScope();

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Non-existing document
		Assertions.assertThatThrownBy(
				() -> query.explain( "InvalidId" )
		)
				.has( new HamcrestCondition<>(
						ExceptionMatcherBuilder.isException( SearchException.class )
								.causedBy( SearchException.class )
								.withMessage(
										"Document with id 'InvalidId' does not exist in the targeted index"
								)
								.withMessage( "its match cannot be explained" )
								.build()
				) );
	}

	@Test
	public void query_explain_multipleIndexes() {
		StubMappingScope scope = indexManager.createScope( otherIndexManager );

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		Assertions.assertThat( query.explain( INDEX_NAME, FIRST_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );

		// Non-matching document
		Assertions.assertThat( query.explain( INDEX_NAME, FIFTH_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void query_explain_multipleIndexes_missingIndexName() {
		StubMappingScope scope = indexManager.createScope( otherIndexManager );

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		Assertions.assertThatThrownBy(
				() -> query.explain( FIRST_ID )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "explain(String id) cannot be used when the query targets multiple indexes" )
				.hasMessageContaining( "pass one of [" + INDEX_NAME + ", " + OTHER_INDEX_NAME + "]" );
	}

	@Test
	public void query_explain_multipleIndexes_invalidIndexName() {
		StubMappingScope scope = indexManager.createScope( otherIndexManager );

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		Assertions.assertThatThrownBy(
				() -> query.explain( "NotAnIndexName", FIRST_ID )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"type name 'NotAnIndexName' is not among the mapped type targeted by this query: ["
						+ INDEX_NAME + ", " + OTHER_INDEX_NAME + "]"
				);
	}

	@Test
	public void predicate_nativeField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "nativeField_dateWithColons" )
						.matching( new JsonPrimitive( "2018:01:12" ) ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FOURTH_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_nativeField_withDslConverter_enabled() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "nativeField_integer_converted" )
						.matching( new ValueWrapper<>( new JsonPrimitive( 2 ) ) ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, SECOND_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_nativeField_withDslConverter_disabled() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "nativeField_integer_converted" )
						.matching( new JsonPrimitive( 2 ), ValueConvert.NO ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, SECOND_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_nativeField_fromJson_jsonObject() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson( gson.fromJson( "{'match': {'nativeField_string': 'text 1'}}", JsonObject.class ) )
						)
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson( gson.fromJson( "{'match': {'nativeField_integer': 2}}", JsonObject.class ) )
						)
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson( gson.fromJson(
										"{"
											+ "'geo_distance': {"
												+ "'distance': '200km',"
												+ "'nativeField_geoPoint': {"
													+ "'lat': 40,"
													+ "'lon': -70"
												+ "}"
											+ "}"
										+ "}",
										JsonObject.class
								)
						)
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID )
				.hasTotalHitCount( 3 );
	}

	@Test
	public void predicate_nativeField_fromJson_jsonObject_separatePredicate() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate1 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( gson.fromJson( "{'match': {'nativeField_string': 'text 1'}}", JsonObject.class ) ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( gson.fromJson( "{'match': {'nativeField_integer': 2}}", JsonObject.class ) ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( gson.fromJson(
						"{"
							+ "'geo_distance': {"
								+ "'distance': '200km',"
								+ "'nativeField_geoPoint': {"
									+ "'lat': 40,"
									+ "'lon': -70"
								+ "}"
							+ "}"
						+ "}",
						JsonObject.class
				) )
				.toPredicate();
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
	public void predicate_nativeField_fromJson_string() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson( "{'match': {'nativeField_string': 'text 1'}}" )
						)
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson( "{'match': {'nativeField_integer': 2}}" )
						)
						.should( f.extension( ElasticsearchExtension.get() )
								.fromJson(
										"{"
											+ "'geo_distance': {"
												+ "'distance': '200km',"
												+ "'nativeField_geoPoint': {"
													+ "'lat': 40,"
													+ "'lon': -70"
												+ "}"
											+ "}"
										+ "}"
								)
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID )
				.hasTotalHitCount( 3 );
	}

	@Test
	public void predicate_nativeField_fromJson_string_separatePredicate() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate1 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( "{'match': {'nativeField_string': 'text 1'}}" ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( "{'match': {'nativeField_integer': 2}}" ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson(
						"{"
							+ "'geo_distance': {"
								+ "'distance': '200km',"
								+ "'nativeField_geoPoint': {"
									+ "'lat': 40,"
									+ "'lon': -70"
								+ "}"
							+ "}"
						+ "}"
				)
				.toPredicate();
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
	public void sort_nativeField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "nativeField_sort1" )
						.then().field( "nativeField_sort2" ).asc()
						.then().field( "nativeField_sort3" )
						.then().field( "nativeField_sort4" ).asc().missing().last()
						.then().field( "nativeField_sort5" ).asc().missing().first()
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID
		);

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "nativeField_sort1" ).desc()
						.then().field( "nativeField_sort2" ).desc()
						.then().field( "nativeField_sort3" ).desc()
						.then().field( "nativeField_sort4" ).desc().missing().last()
						.then().field( "nativeField_sort5" ).asc().missing().first()
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_nativeField_jsonObject() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f
						.extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort1': 'asc'}", JsonObject.class
						) )
						.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort2': 'asc'}", JsonObject.class
						) )
						.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort3': 'asc'}", JsonObject.class
						) )
						.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort4': {'order': 'asc', 'missing': '_last'}}", JsonObject.class
						) )
						.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort5': {'order': 'asc', 'missing': '_first'}}", JsonObject.class
						) )
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID
		);

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f
						.extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort1': 'desc'}", JsonObject.class
						) )
						.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort2': 'desc'}", JsonObject.class
						) )
						.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort3': 'desc'}", JsonObject.class
						) )
						.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort4': {'order': 'desc', 'missing': '_last'}}", JsonObject.class
						) )
						.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
								"{'nativeField_sort5': {'order': 'asc', 'missing': '_first'}}", JsonObject.class
						) )
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_nativeField_fromJson_jsonObject_separateSort() {
		StubMappingScope scope = indexManager.createScope();

		SearchSort sort1Asc = scope.sort().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort1': 'asc'}", JsonObject.class
				) )
				.toSort();
		SearchSort sort2Asc = scope.sort().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort2': 'asc'}", JsonObject.class
				) )
				.toSort();
		SearchSort sort3Asc = scope.sort().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort3': 'asc'}", JsonObject.class
				) )
				.toSort();
		SearchSort sort4Asc = scope.sort()
				.extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort4': {'order': 'asc', 'missing': '_last'}}", JsonObject.class
				) )
				.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort5': {'order': 'asc', 'missing': '_first'}}", JsonObject.class
				) )
				.toSort();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.composite().add( sort1Asc ).add( sort2Asc ).add( sort3Asc ).add( sort4Asc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID );

		SearchSort sort1Desc = scope.sort().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort1': 'desc'}", JsonObject.class
				) )
				.toSort();
		SearchSort sort2Desc = scope.sort().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort2': 'desc'}", JsonObject.class
				) )
				.toSort();
		SearchSort sort3Desc = scope.sort().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort3': 'desc'}", JsonObject.class
				) )
				.toSort();
		SearchSort sort4Desc = scope.sort()
				.extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort4': {'order': 'desc', 'missing': '_last'}}", JsonObject.class
				) )
				.then().extension( ElasticsearchExtension.get() ).fromJson( gson.fromJson(
						"{'nativeField_sort5': {'order': 'asc', 'missing': '_first'}}", JsonObject.class
				) )
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.composite().add( sort1Desc ).add( sort2Desc ).add( sort3Desc ).add( sort4Desc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID );
	}

	@Test
	public void sort_nativeField_fromJson_string() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f
						.extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort1': 'asc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort2': 'asc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort3': 'asc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort4': {'order': 'asc', 'missing': '_last'}}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort5': {'order': 'asc', 'missing': '_first'}}" )
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID
		);

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f
						.extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort1': 'desc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort2': 'desc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort3': 'desc'}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort4': {'order': 'desc', 'missing': '_last'}}" )
						.then().extension( ElasticsearchExtension.get() )
								.fromJson( "{'nativeField_sort5': {'order': 'asc', 'missing': '_first'}}" )
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_nativeField_fromJson_string_separateSort() {
		StubMappingScope scope = indexManager.createScope();

		SearchSort sort1Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'nativeField_sort1': 'asc'}" )
				.toSort();
		SearchSort sort2Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'nativeField_sort2': 'asc'}" )
				.toSort();
		SearchSort sort3Asc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'nativeField_sort3': 'asc'}" )
				.toSort();
		SearchSort sort4Asc = scope.sort()
				.extension( ElasticsearchExtension.get() )
						.fromJson( "{'nativeField_sort4': {'order': 'asc', 'missing': '_last'}}" )
				.then().extension( ElasticsearchExtension.get() )
						.fromJson( "{'nativeField_sort5': {'order': 'asc', 'missing': '_first'}}" )
				.toSort();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.composite().add( sort1Asc ).add( sort2Asc ).add( sort3Asc ).add( sort4Asc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID );

		SearchSort sort1Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'nativeField_sort1': 'desc'}" )
				.toSort();
		SearchSort sort2Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'nativeField_sort2': 'desc'}" )
				.toSort();
		SearchSort sort3Desc = scope.sort().extension( ElasticsearchExtension.get() )
				.fromJson( "{'nativeField_sort3': 'desc'}" )
				.toSort();
		SearchSort sort4Desc = scope.sort()
				.extension( ElasticsearchExtension.get() )
						.fromJson( "{'nativeField_sort4': {'order': 'desc', 'missing': '_last'}}" )
				.then().extension( ElasticsearchExtension.get() )
						.fromJson( "{'nativeField_sort5': {'order': 'asc', 'missing': '_first'}}" )
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.composite().add( sort1Desc ).add( sort2Desc ).add( sort3Desc ).add( sort4Desc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID );
	}

	@Test
	public void sort_filter_fromJson() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( indexMapping.nestedObject.relativeFieldName + ".sort1" )
						// The provided predicate factory should already be extended and offer Elasticsearch-specific extensions
						.filter( pf -> pf.fromJson(
								"{'match': {'"
										+ indexMapping.nestedObject.relativeFieldName + ".discriminator"
										+ "': 'included'}}"
						) )
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID
		);

		// Check descending order, just in case the above order was reached by chance.
		query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( indexMapping.nestedObject.relativeFieldName + ".sort1" )
						.desc()
						.filter( pf -> pf.fromJson(
								"{'match': {'"
										+ indexMapping.nestedObject.relativeFieldName + ".discriminator"
										+ "': 'included'}}"
						) )
				)
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder(
				INDEX_NAME,
				FIFTH_ID, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID
		);
	}

	@Test
	public void projection_nativeField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<JsonElement> query = scope.query()
				.select( f -> f.field( "nativeField_integer", JsonElement.class ) )
				.where( f -> f.id().matching( SECOND_ID ) )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( new JsonPrimitive( 2 ) );
	}

	@Test
	public void projection_nativeField_withProjectionConverters_enabled() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<ValueWrapper> query = scope.query()
				.select( f -> f.field( "nativeField_integer_converted", ValueWrapper.class ) )
				.where( f -> f.id().matching( SECOND_ID ) )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( new ValueWrapper<>( new JsonPrimitive( 2 ) ) );
	}

	@Test
	public void projection_nativeField_withProjectionConverters_disabled() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<JsonElement> query = scope.query()
				.select( f -> f.field( "nativeField_integer_converted", JsonElement.class, ValueConvert.NO ) )
				.where( f -> f.id().matching( SECOND_ID ) )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder( new JsonPrimitive( 2 ) );
	}

	@Test
	public void projection_document() throws JSONException {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<JsonObject> query = scope.query()
				.select(
						f -> f.extension( ElasticsearchExtension.get() ).source()
				)
				.where( f -> f.id().matching( FIFTH_ID ) )
				.toQuery();

		List<JsonObject> result = query.fetchAll().getHits();
		Assertions.assertThat( result ).hasSize( 1 );
		assertJsonEquals(
				"{"
						+ "'string': 'text 5',"
						+ "'nativeField_string': 'text 2',"
						+ "'nativeField_integer': 1,"
						+ "'nativeField_geoPoint': {'lat': 45.12, 'lon': -75.34},"
						+ "'nativeField_dateWithColons': '2018:01:25',"
						+ "'nativeField_unsupportedType': 'foobar',"
						+ "'nativeField_sort5': 'z'"
						+ "}",
				result.get( 0 ).toString(),
				JSONCompareMode.LENIENT
		);
	}

	/**
	 * Check that the projection on source includes all fields,
	 * even if there is a field projection, which would usually trigger source filtering.
	 */
	@Test
	public void projection_documentAndField() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select( f ->
						f.composite(
								f.extension( ElasticsearchExtension.get() ).source(),
								f.field( "nativeField_string" )
						)
				)
				.where( f -> f.id().matching( FIFTH_ID ) )
				.toQuery();

		List<JsonObject> result = query.fetchAll().getHits().stream()
				.map( list -> (JsonObject) list.get( 0 ) )
				.collect( Collectors.toList() );
		Assertions.assertThat( result ).hasSize( 1 );
		assertJsonEquals(
				"{"
						+ "'string': 'text 5',"
						+ "'nativeField_string': 'text 2',"
						+ "'nativeField_integer': 1,"
						+ "'nativeField_geoPoint': {'lat': 45.12, 'lon': -75.34},"
						+ "'nativeField_dateWithColons': '2018:01:25',"
						+ "'nativeField_unsupportedType': 'foobar',"
						+ "'nativeField_sort5': 'z'"
						+ "}",
				result.get( 0 ).toString(),
				JSONCompareMode.LENIENT
		);
	}

	@Test
	public void projection_explanation() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<JsonObject> query = scope.query()
				.select( f -> f.extension( ElasticsearchExtension.get() ).explanation() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<JsonObject> result = query.fetchAll().getHits();
		Assertions.assertThat( result ).hasSize( 1 );
		Assertions.assertThat( result.get( 0 ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void projection_jsonHit() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<JsonObject> query = scope.query()
				.select( f -> f.extension( ElasticsearchExtension.get() ).jsonHit() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<JsonObject> result = query.fetchAll().getHits();
		Assertions.assertThat( result ).hasSize( 1 );
		assertJsonEquals(
				"{"
						+ "'_id': '" + FIRST_ID + "',"
						+ "'_index': '" + defaultPrimaryName( INDEX_NAME ) + "'"
						+ "}",
				result.get( 0 ).toString(),
				JSONCompareMode.LENIENT
		);
	}

	@Test
	public void aggregation_nativeField() {
		StubMappingScope scope = indexManager.createScope();

		AggregationKey<Map<JsonElement, Long>> documentCountPerValue = AggregationKey.of( "documentCountPerValue" );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.aggregation( documentCountPerValue, f -> f.terms().field( "nativeField_aggregation", JsonElement.class ) )
				.toQuery();
		assertThat( query ).aggregation( documentCountPerValue )
				.asInstanceOf( InstanceOfAssertFactories.map( JsonElement.class, Long.class ) )
				.containsExactly(
						// There are extra quotes because it's a native field: these are JSON-formatted strings representing string values
						Assertions.entry( new JsonPrimitive( "value-for-doc-1-and-2" ), 2L ),
						Assertions.entry( new JsonPrimitive( "value-for-doc-3" ), 1L )
				);
	}

	@Test
	public void aggregation_nativeField_fromJson_jsonObject() {
		StubMappingScope scope = indexManager.createScope();

		AggregationKey<JsonObject> documentCountPerValue = AggregationKey.of( "documentCountPerValue" );

		SearchQuery<DocumentReference> query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.aggregation( documentCountPerValue, f -> f.fromJson( gson.fromJson(
						"{"
								+ "'value_count' : {"
										+ "'field' : 'nativeField_aggregation'"
								+ " }"
								+ "}",
						JsonObject.class
				) ) )
				.toQuery();
		JsonObject aggregationResult = query.fetchAll().getAggregation( documentCountPerValue );
		assertJsonEquals(
				"{"
						+ "'value': 3"
						+ "}",
				aggregationResult.toString()
		);
	}


	@Test
	public void aggregation_nativeField_fromJson_string() {
		StubMappingScope scope = indexManager.createScope();

		AggregationKey<JsonObject> documentCountPerValue = AggregationKey.of( "documentCountPerValue" );

		SearchQuery<DocumentReference> query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.aggregation( documentCountPerValue, f -> f.fromJson(
						"{"
								+ "'value_count' : {"
										+ "'field' : 'nativeField_aggregation'"
								+ " }"
								+ "}"
				) )
				.toQuery();
		JsonObject aggregationResult = query.fetchAll().getAggregation( documentCountPerValue );
		assertJsonEquals(
				"{"
						+ "'value': 3"
						+ "}",
				aggregationResult.toString()
		);
	}


	@Test
	public void aggregation_filter_fromLuceneQuery() {
		StubMappingScope scope = indexManager.createScope();

		AggregationKey<Map<String, Long>> aggregationKey = AggregationKey.of( "agg" );

		SearchQuery<DocumentReference> query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.aggregation( aggregationKey, f -> f.terms()
						.field( indexMapping.nestedObject.relativeFieldName + ".aggregation1", String.class )
						// The provided predicate factory should already be extended and offer Elasticsearch-specific extensions
						.filter( pf -> pf.fromJson(
								"{'match': {'"
										+ indexMapping.nestedObject.relativeFieldName + ".discriminator"
										+ "': 'included'}}"
						) )
				)
				.toQuery();
		assertThat( query ).aggregation( aggregationKey, agg -> Assertions.assertThat( agg ).containsExactly(
				entry( "five", 1L ),
				entry( "four", 1L ),
				entry( "one", 1L ),
				entry( "three", 1L ),
				entry( "two", 1L )
		) );
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

		assertThatThrownBy( () -> backend.unwrap( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Attempt to unwrap an Elasticsearch backend to '" + String.class.getName() + "'",
						"this backend can only be unwrapped to '" + ElasticsearchBackend.class.getName() + "'"
				);
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

		assertThatThrownBy( () -> elasticsearchBackend.getClient( HttpAsyncClient.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						HttpAsyncClient.class.getName(),
						"the client can only be unwrapped to",
						RestClient.class.getName()
				);
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

		assertThatThrownBy( () -> indexManager.unwrap( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Attempt to unwrap an Elasticsearch index manager to '" + String.class.getName() + "'",
						"this index manager can only be unwrapped to '" + ElasticsearchIndexManager.class.getName() + "'"
				);
	}

	private void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( SECOND_ID ), document -> {
			document.addValue( indexMapping.string, "text 2" );

			document.addValue( indexMapping.nativeField_integer, new JsonPrimitive( 2 ) );
			document.addValue( indexMapping.nativeField_integer_converted, new JsonPrimitive( 2 ) );
			document.addValue( indexMapping.nativeField_unsupportedType, new JsonPrimitive( "42" ) );

			document.addValue( indexMapping.nativeField_sort1, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort2, new JsonPrimitive( "a" ) );
			document.addValue( indexMapping.nativeField_sort3, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort4, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort5, new JsonPrimitive( "a" ) );

			document.addValue( indexMapping.nativeField_aggregation, new JsonPrimitive( "value-for-doc-1-and-2" ) );

			DocumentElement nestedObject1 = document.addObject( indexMapping.nestedObject.self );
			nestedObject1.addValue( indexMapping.nestedObject.discriminator, "included" );
			nestedObject1.addValue( indexMapping.nestedObject.sort1, "b" );
			nestedObject1.addValue( indexMapping.nestedObject.aggregation1, "one" );
			DocumentElement nestedObject2 = document.addObject( indexMapping.nestedObject.self );
			nestedObject2.addValue( indexMapping.nestedObject.discriminator, "excluded" );
			nestedObject2.addValue( indexMapping.nestedObject.sort1, "a" );
			nestedObject2.addValue( indexMapping.nestedObject.aggregation1, "fifty-one" );
		} );
		plan.add( referenceProvider( FIRST_ID ), document -> {
			document.addValue( indexMapping.string, "text 1" );

			document.addValue( indexMapping.nativeField_string, new JsonPrimitive( "text 1" ) );

			document.addValue( indexMapping.nativeField_sort1, new JsonPrimitive( "a" ) );
			document.addValue( indexMapping.nativeField_sort2, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort3, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort4, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort5, new JsonPrimitive( "a" ) );

			document.addValue( indexMapping.nativeField_aggregation, new JsonPrimitive( "value-for-doc-1-and-2" ) );

			DocumentElement nestedObject1 = document.addObject( indexMapping.nestedObject.self );
			nestedObject1.addValue( indexMapping.nestedObject.discriminator, "included" );
			nestedObject1.addValue( indexMapping.nestedObject.sort1, "a" );
			nestedObject1.addValue( indexMapping.nestedObject.aggregation1, "two" );
			DocumentElement nestedObject2 = document.addObject( indexMapping.nestedObject.self );
			nestedObject2.addValue( indexMapping.nestedObject.discriminator, "excluded" );
			nestedObject2.addValue( indexMapping.nestedObject.sort1, "b" );
			nestedObject2.addValue( indexMapping.nestedObject.aggregation1, "fifty-two" );
		} );
		plan.add( referenceProvider( THIRD_ID ), document -> {
			document.addValue( indexMapping.string, "text 3" );

			document.addValue( indexMapping.nativeField_geoPoint, gson.fromJson( "{'lat': 40.12, 'lon': -71.34}", JsonObject.class ) );

			document.addValue( indexMapping.nativeField_sort1, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort2, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort3, new JsonPrimitive( "a" ) );
			document.addValue( indexMapping.nativeField_sort4, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort5, new JsonPrimitive( "a" ) );

			document.addValue( indexMapping.nativeField_aggregation, new JsonPrimitive( "value-for-doc-3" ) );

			DocumentElement nestedObject1 = document.addObject( indexMapping.nestedObject.self );
			nestedObject1.addValue( indexMapping.nestedObject.discriminator, "included" );
			nestedObject1.addValue( indexMapping.nestedObject.sort1, "c" );
			nestedObject1.addValue( indexMapping.nestedObject.aggregation1, "three" );
			DocumentElement nestedObject2 = document.addObject( indexMapping.nestedObject.self );
			nestedObject2.addValue( indexMapping.nestedObject.discriminator, "excluded" );
			nestedObject2.addValue( indexMapping.nestedObject.sort1, "b" );
			nestedObject2.addValue( indexMapping.nestedObject.aggregation1, "fifty-three" );
		} );
		plan.add( referenceProvider( FOURTH_ID ), document -> {
			document.addValue( indexMapping.string, "text 4" );

			document.addValue( indexMapping.nativeField_dateWithColons, new JsonPrimitive( "2018:01:12" ) );

			document.addValue( indexMapping.nativeField_sort1, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort2, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort3, new JsonPrimitive( "z" ) );
			document.addValue( indexMapping.nativeField_sort4, new JsonPrimitive( "a" ) );
			document.addValue( indexMapping.nativeField_sort5, new JsonPrimitive( "a" ) );

			DocumentElement nestedObject1 = document.addObject( indexMapping.nestedObject.self );
			nestedObject1.addValue( indexMapping.nestedObject.discriminator, "included" );
			nestedObject1.addValue( indexMapping.nestedObject.sort1, "d" );
			nestedObject1.addValue( indexMapping.nestedObject.aggregation1, "four" );
			DocumentElement nestedObject2 = document.addObject( indexMapping.nestedObject.self );
			nestedObject2.addValue( indexMapping.nestedObject.discriminator, "excluded" );
			nestedObject2.addValue( indexMapping.nestedObject.sort1, "c" );
			nestedObject2.addValue( indexMapping.nestedObject.aggregation1, "fifty-four" );
		} );
		plan.add( referenceProvider( FIFTH_ID ), document -> {
			document.addValue( indexMapping.string, "text 5" );

			// This document should not match any query
			document.addValue( indexMapping.nativeField_string, new JsonPrimitive( "text 2" ) );
			document.addValue( indexMapping.nativeField_integer, new JsonPrimitive( 1 ) );
			document.addValue( indexMapping.nativeField_geoPoint, gson.fromJson( "{'lat': 45.12, 'lon': -75.34}", JsonObject.class ) );
			document.addValue( indexMapping.nativeField_dateWithColons, new JsonPrimitive( "2018:01:25" ) );
			document.addValue( indexMapping.nativeField_unsupportedType, new JsonPrimitive( "foobar" ) ); // ignore_malformed is enabled, this should be ignored

			document.addValue( indexMapping.nativeField_sort5, new JsonPrimitive( "z" ) );

			DocumentElement nestedObject1 = document.addObject( indexMapping.nestedObject.self );
			nestedObject1.addValue( indexMapping.nestedObject.discriminator, "included" );
			nestedObject1.addValue( indexMapping.nestedObject.sort1, "e" );
			nestedObject1.addValue( indexMapping.nestedObject.aggregation1, "five" );
			DocumentElement nestedObject2 = document.addObject( indexMapping.nestedObject.self );
			nestedObject2.addValue( indexMapping.nestedObject.discriminator, "excluded" );
			nestedObject2.addValue( indexMapping.nestedObject.sort1, "a" );
			nestedObject2.addValue( indexMapping.nestedObject.aggregation1, "fifty-five" );
		} );
		plan.add( referenceProvider( EMPTY_ID ), document -> { } );

		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder(
				INDEX_NAME,
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID
		);
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;
		final IndexFieldReference<JsonElement> nativeField_integer;
		final IndexFieldReference<JsonElement> nativeField_integer_converted;
		final IndexFieldReference<JsonElement> nativeField_string;
		final IndexFieldReference<JsonElement> nativeField_geoPoint;
		final IndexFieldReference<JsonElement> nativeField_dateWithColons;
		final IndexFieldReference<JsonElement> nativeField_unsupportedType;

		final IndexFieldReference<JsonElement> nativeField_sort1;
		final IndexFieldReference<JsonElement> nativeField_sort2;
		final IndexFieldReference<JsonElement> nativeField_sort3;
		final IndexFieldReference<JsonElement> nativeField_sort4;
		final IndexFieldReference<JsonElement> nativeField_sort5;

		final IndexFieldReference<JsonElement> nativeField_aggregation;

		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			nativeField_integer = root.field(
					"nativeField_integer",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'integer'}" )
			)
					.toReference();
			nativeField_integer_converted = root.field(
					"nativeField_integer_converted",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'integer'}" )
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromIndexFieldConverter() )
			)
					.toReference();
			nativeField_string = root.field(
					"nativeField_string",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'keyword'}" )
			)
					.toReference();
			nativeField_geoPoint = root.field(
					"nativeField_geoPoint",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'geo_point'}" )
			)
					.toReference();
			nativeField_dateWithColons = root.field(
					"nativeField_dateWithColons",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'date', 'format': 'yyyy:MM:dd'}" )
			)
					.toReference();
			nativeField_unsupportedType = root.field(
					"nativeField_unsupportedType",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'half_float', 'ignore_malformed': true}" )
			)
					.toReference();

			nativeField_sort1 = root.field(
					"nativeField_sort1",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
			nativeField_sort2 = root.field(
					"nativeField_sort2",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
			nativeField_sort3 = root.field(
					"nativeField_sort3",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
			nativeField_sort4 = root.field(
					"nativeField_sort4",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();
			nativeField_sort5 = root.field(
					"nativeField_sort5",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();

			nativeField_aggregation = root.field(
					"nativeField_aggregation",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{'type': 'keyword', 'doc_values': true}" )
			)
					.toReference();

			nestedObject = ObjectMapping.create( root, "nestedObject", ObjectFieldStorage.NESTED, true );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;
		final IndexFieldReference<String> sort1;
		final IndexFieldReference<String> aggregation1;

		public static ObjectMapping create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage,
				boolean multiValued) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
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
		}
	}
}