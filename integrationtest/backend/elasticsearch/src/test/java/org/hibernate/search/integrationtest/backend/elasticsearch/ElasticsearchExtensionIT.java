/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScroll;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScrollResult;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryOptionsStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQuerySelectStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryWhereStep;
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
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.http.nio.client.HttpAsyncClient;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class ElasticsearchExtensionIT {

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String FOURTH_ID = "4";
	private static final String FIFTH_ID = "5";
	private static final String EMPTY_ID = "empty";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<IndexBinding> otherIndex = SimpleMappedIndex.of( IndexBinding::new ).name( "other" );

	private final Gson gson = new Gson();

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
		ElasticsearchSearchQuerySelectStep<EntityReference, DocumentReference, StubLoadingOptionsStep> context1 =
				scope.query().extension( ElasticsearchExtension.get() );
		ElasticsearchSearchQueryWhereStep<DocumentReference, StubLoadingOptionsStep> context2 = context1.select(
				f -> f.composite()
						.from( f.documentReference(), f.source() )
						// We don't care about the source, it's just to test that the factory context allows ES-specific projection
						.as( (docRef, source) -> docRef )
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

		assertThatResult( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID )
				.hasTotalHitCount( 6 );

		// Also check (at compile time) the context type for other asXXX() methods, since we need to override each method explicitly
		ElasticsearchSearchQueryWhereStep<EntityReference, StubLoadingOptionsStep> selectEntityReferenceContext =
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
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> genericQuery = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Put the query and result into variables to check they have the right type
		ElasticsearchSearchQuery<DocumentReference> query = genericQuery.extension( ElasticsearchExtension.get() );
		ElasticsearchSearchResult<DocumentReference> result = query.fetchAll();
		assertThatResult( result ).fromQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID )
				.hasTotalHitCount( 6 );

		// Unsupported extension
		assertThatThrownBy(
				() -> query.extension(
						(SearchQuery<DocumentReference> original, SearchLoadingContext<?> loadingContext) -> Optional.empty() )
		)
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void query_gsonResponseBody() {
		StubMappingScope scope = mainIndex.createScope();

		ElasticsearchSearchResult<DocumentReference> result = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.fetchAll();

		assertThat( result.responseBody() )
				.isNotNull()
				.extracting( body -> body.get( "_shards" ) ).isInstanceOf( JsonObject.class );
	}

	@Test
	public void query_explain_singleIndex() {
		StubMappingScope scope = mainIndex.createScope();

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		assertThat( query.explain( FIRST_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );

		// Non-matching document
		assertThat( query.explain( FIFTH_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3783")
	public void query_explain_projection() {
		StubMappingScope scope = mainIndex.createScope();

		ElasticsearchSearchQuery<String> query = scope.query().extension( ElasticsearchExtension.get() )
				.select( f -> f.field( "string", String.class ) )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		assertThat( query.explain( FIRST_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );

		// Non-matching document
		assertThat( query.explain( FIFTH_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void query_explain_singleIndex_invalidId() {
		StubMappingScope scope = mainIndex.createScope();

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Non-existing document
		assertThatThrownBy( () -> query.explain( "InvalidId" ) )
				.isInstanceOf( SearchException.class )
				.extracting( Throwable::getCause, InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid document identifier: 'InvalidId'",
						"No such document in index 'main-read'" );
	}

	@Test
	public void query_explain_multipleIndexes() {
		StubMappingScope scope = mainIndex.createScope( otherIndex );

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		// Matching document
		assertThat( query.explain( mainIndex.typeName(), FIRST_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );

		// Non-matching document
		assertThat( query.explain( mainIndex.typeName(), FIFTH_ID ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void query_explain_multipleIndexes_missingTypeName() {
		StubMappingScope scope = mainIndex.createScope( otherIndex );

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
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

		ElasticsearchSearchQuery<DocumentReference> query = scope.query().extension( ElasticsearchExtension.get() )
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
		try ( ElasticsearchSearchScroll<DocumentReference> scroll = mainIndex.query()
				.extension( ElasticsearchExtension.get() ) // Call extension() on the DSL step
				.where( f -> f.matchAll() )
				.scroll( 20 ) ) { // Call scroll() on the fetchable
			List<DocumentReference> hits = new ArrayList<>();
			// Check the scroll result has the extended type and works correctly
			for ( ElasticsearchSearchScrollResult<DocumentReference> chunk = scroll.next(); chunk.hasHits();
					chunk = scroll.next() ) {
				hits.addAll( chunk.hits() );
				assertThat( chunk.total().hitCount() ).isEqualTo( 6 );
			}
			assertThatHits( hits )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(),
							FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3974")
	public void scroll_onQuery() {
		// Check the scroll has the extended type and works correctly
		try ( ElasticsearchSearchScroll<DocumentReference> scroll = mainIndex.query()
				.where( f -> f.matchAll() )
				.toQuery()
				.extension( ElasticsearchExtension.get() ) // Call extension() on the query
				.scroll( 20 ) ) { // Call scroll() on the query
			List<DocumentReference> hits = new ArrayList<>();
			// Check the scroll result has the extended type and works correctly
			for ( ElasticsearchSearchScrollResult<DocumentReference> chunk = scroll.next(); chunk.hasHits();
					chunk = scroll.next() ) {
				hits.addAll( chunk.hits() );
				assertThat( chunk.total().hitCount() ).isEqualTo( 6 );
			}
			assertThatHits( hits )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(),
							FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID );
		}
	}

	@Test
	public void predicate_nativeField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "nativeField_dateWithColons" )
						.matching( new JsonPrimitive( "2018:01:12" ) ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FOURTH_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_nativeField_withDslConverter_enabled() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "nativeField_integer_converted" )
						.matching( new ValueWrapper<>( new JsonPrimitive( 2 ) ) ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), SECOND_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_nativeField_withDslConverter_disabled() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "nativeField_integer_converted" )
						.matching( new JsonPrimitive( 2 ), ValueConvert.NO ) )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), SECOND_ID )
				.hasTotalHitCount( 1 );
	}

	@Test
	public void predicate_nativeField_fromJson_jsonObject() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.or(
						f.extension( ElasticsearchExtension.get() )
								.fromJson( gson.fromJson( "{'match': {'nativeField_string': 'text 1'}}", JsonObject.class ) ),
						f.extension( ElasticsearchExtension.get() )
								.fromJson( gson.fromJson( "{'match': {'nativeField_integer': 2}}", JsonObject.class ) ),
						f.extension( ElasticsearchExtension.get() )
								.fromJson( gson.fromJson(
										"{"
												+ " 'geo_distance': {"
												+ "  'distance': '200km',"
												+ "  'nativeField_geoPoint': {"
												+ "   'lat': 40,"
												+ "   'lon': -70"
												+ "  }"
												+ " }"
												+ "}",
										JsonObject.class
								)
								)
				)
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID )
				.hasTotalHitCount( 3 );
	}

	@Test
	public void predicate_nativeField_fromJson_jsonObject_separatePredicate() {
		StubMappingScope scope = mainIndex.createScope();

		SearchPredicate predicate1 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( gson.fromJson( "{'match': {'nativeField_string': 'text 1'}}", JsonObject.class ) ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( gson.fromJson( "{'match': {'nativeField_integer': 2}}", JsonObject.class ) ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( gson.fromJson(
						"{"
								+ " 'geo_distance': {"
								+ "  'distance': '200km',"
								+ "  'nativeField_geoPoint': {"
								+ "   'lat': 40,"
								+ "   'lon': -70"
								+ "  }"
								+ " }"
								+ "}",
						JsonObject.class
				) )
				.toPredicate();
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
	public void predicate_nativeField_fromJson_string() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.or(
						f.extension( ElasticsearchExtension.get() )
								.fromJson( "{'match': {'nativeField_string': 'text 1'}}" ),
						f.extension( ElasticsearchExtension.get() )
								.fromJson( "{'match': {'nativeField_integer': 2}}" ),
						f.extension( ElasticsearchExtension.get() )
								.fromJson(
										"{"
												+ " 'geo_distance': {"
												+ "  'distance': '200km',"
												+ "  'nativeField_geoPoint': {"
												+ "   'lat': 40,"
												+ "   'lon': -70"
												+ "  }"
												+ " }"
												+ "}"
								)
				)
				)
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID )
				.hasTotalHitCount( 3 );
	}

	@Test
	public void predicate_nativeField_fromJson_string_separatePredicate() {
		StubMappingScope scope = mainIndex.createScope();

		SearchPredicate predicate1 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( "{'match': {'nativeField_string': 'text 1'}}" ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson( "{'match': {'nativeField_integer': 2}}" ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().extension( ElasticsearchExtension.get() )
				.fromJson(
						"{"
								+ " 'geo_distance': {"
								+ "  'distance': '200km',"
								+ "  'nativeField_geoPoint': {"
								+ "   'lat': 40,"
								+ "   'lon': -70"
								+ "  }"
								+ " }"
								+ "}"
				)
				.toPredicate();
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
	public void sort_nativeField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "nativeField_sort1" )
						.then().field( "nativeField_sort2" ).asc()
						.then().field( "nativeField_sort3" )
						.then().field( "nativeField_sort4" ).asc().missing().last()
						.then().field( "nativeField_sort5" ).asc().missing().first()
				)
				.toQuery();
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
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
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_nativeField_jsonObject() {
		StubMappingScope scope = mainIndex.createScope();

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
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
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
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_nativeField_fromJson_jsonObject_separateSort() {
		StubMappingScope scope = mainIndex.createScope();

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
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID );

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
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID );
	}

	@Test
	public void sort_nativeField_fromJson_string() {
		StubMappingScope scope = mainIndex.createScope();

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
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
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
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID
		);
	}

	@Test
	public void sort_nativeField_fromJson_string_separateSort() {
		StubMappingScope scope = mainIndex.createScope();

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
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, EMPTY_ID, FIFTH_ID );

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
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID, FIFTH_ID );
	}

	@Test
	public void sort_filter_fromJson() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( mainIndex.binding().nestedObject.relativeFieldName + ".sort1" )
						// The provided predicate factory should already be extended and offer Elasticsearch-specific extensions
						.filter( pf -> pf.fromJson(
								"{'match': {'"
										+ mainIndex.binding().nestedObject.relativeFieldName + ".discriminator"
										+ "': 'included'}}"
						) )
				)
				.toQuery();
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				FIRST_ID, SECOND_ID, THIRD_ID, FOURTH_ID, FIFTH_ID, EMPTY_ID
		);

		// Check descending order, just in case the above order was reached by chance.
		query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( mainIndex.binding().nestedObject.relativeFieldName + ".sort1" )
						.desc()
						.filter( pf -> pf.fromJson(
								"{'match': {'"
										+ mainIndex.binding().nestedObject.relativeFieldName + ".discriminator"
										+ "': 'included'}}"
						) )
				)
				.toQuery();
		assertThatQuery( query ).hasDocRefHitsExactOrder(
				mainIndex.typeName(),
				FIFTH_ID, FOURTH_ID, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID
		);
	}

	@Test
	public void projection_nativeField() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<JsonElement> query = scope.query()
				.select( f -> f.field( "nativeField_integer", JsonElement.class ) )
				.where( f -> f.id().matching( SECOND_ID ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( new JsonPrimitive( 2 ) );
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void projection_nativeField_withProjectionConverters_enabled() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<ValueWrapper> query = scope.query()
				.select( f -> f.field( "nativeField_integer_converted", ValueWrapper.class ) )
				.where( f -> f.id().matching( SECOND_ID ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( new ValueWrapper<>( new JsonPrimitive( 2 ) ) );
	}

	@Test
	public void projection_nativeField_withProjectionConverters_disabled() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<JsonElement> query = scope.query()
				.select( f -> f.field( "nativeField_integer_converted", JsonElement.class, ValueConvert.NO ) )
				.where( f -> f.id().matching( SECOND_ID ) )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( new JsonPrimitive( 2 ) );
	}

	@Test
	public void projection_document() throws JSONException {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<JsonObject> query = scope.query()
				.select(
						f -> f.extension( ElasticsearchExtension.get() ).source()
				)
				.where( f -> f.id().matching( FIFTH_ID ) )
				.toQuery();

		List<JsonObject> result = query.fetchAll().hits();
		assertThat( result ).hasSize( 1 );
		assertJsonEquals(
				"{"
						+ "  'string': 'text 5',"
						+ "  'nativeField_string': 'text 2',"
						+ "  'nativeField_integer': 1,"
						+ "  'nativeField_geoPoint': {'lat': 45.12, 'lon': -75.34},"
						+ "  'nativeField_dateWithColons': '2018:01:25',"
						+ "  'nativeField_unsupportedType': 'foobar',"
						+ "  'nativeField_sort5': 'z'"
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
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select( f -> f.composite(
						f.extension( ElasticsearchExtension.get() ).source(),
						f.field( "nativeField_string" )
				)
				)
				.where( f -> f.id().matching( FIFTH_ID ) )
				.toQuery();

		List<JsonObject> result = query.fetchAll().hits().stream()
				.map( list -> (JsonObject) list.get( 0 ) )
				.collect( Collectors.toList() );
		assertThat( result ).hasSize( 1 );
		assertJsonEquals(
				"{"
						+ "  'string': 'text 5',"
						+ "  'nativeField_string': 'text 2',"
						+ "  'nativeField_integer': 1,"
						+ "  'nativeField_geoPoint': {'lat': 45.12, 'lon': -75.34},"
						+ "  'nativeField_dateWithColons': '2018:01:25',"
						+ "  'nativeField_unsupportedType': 'foobar',"
						+ "  'nativeField_sort5': 'z'"
						+ "}",
				result.get( 0 ).toString(),
				JSONCompareMode.LENIENT
		);
	}

	@Test
	public void projection_explanation() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<JsonObject> query = scope.query()
				.select( f -> f.extension( ElasticsearchExtension.get() ).explanation() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<JsonObject> result = query.fetchAll().hits();
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) )
				.asString()
				.contains( "\"description\":" )
				.contains( "\"details\":" );
	}

	@Test
	public void projection_jsonHit() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<JsonObject> query = scope.query()
				.select( f -> f.extension( ElasticsearchExtension.get() ).jsonHit() )
				.where( f -> f.id().matching( FIRST_ID ) )
				.toQuery();

		List<JsonObject> result = query.fetchAll().hits();
		assertThat( result ).hasSize( 1 );
		assertJsonEquals(
				"{"
						+ "  '_id': '" + FIRST_ID + "',"
						+ "  '_index': '" + defaultPrimaryName( mainIndex.name() ) + "'"
						+ "}",
				result.get( 0 ).toString(),
				JSONCompareMode.LENIENT
		);
	}

	@Test
	public void aggregation_nativeField() {
		StubMappingScope scope = mainIndex.createScope();

		AggregationKey<Map<JsonElement, Long>> documentCountPerValue = AggregationKey.of( "documentCountPerValue" );

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.aggregation( documentCountPerValue, f -> f.terms().field( "nativeField_aggregation", JsonElement.class ) )
				.toQuery();
		assertThatQuery( query ).aggregation( documentCountPerValue )
				.asInstanceOf( InstanceOfAssertFactories.map( JsonElement.class, Long.class ) )
				.containsExactly(
						// There are extra quotes because it's a native field: these are JSON-formatted strings representing string values
						entry( new JsonPrimitive( "value-for-doc-1-and-2" ), 2L ),
						entry( new JsonPrimitive( "value-for-doc-3" ), 1L )
				);
	}

	@Test
	public void aggregation_nativeField_fromJson_jsonObject() {
		StubMappingScope scope = mainIndex.createScope();

		AggregationKey<JsonObject> documentCountPerValue = AggregationKey.of( "documentCountPerValue" );

		SearchQuery<DocumentReference> query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.aggregation( documentCountPerValue, f -> f.fromJson( gson.fromJson(
						"{"
								+ "  'value_count' : {"
								+ "    'field' : 'nativeField_aggregation'"
								+ " }"
								+ "}",
						JsonObject.class
				) ) )
				.toQuery();
		JsonObject aggregationResult = query.fetchAll().aggregation( documentCountPerValue );
		assertJsonEquals(
				"{"
						+ "  'value': 3"
						+ "}",
				aggregationResult.toString()
		);
	}


	@Test
	public void aggregation_nativeField_fromJson_string() {
		StubMappingScope scope = mainIndex.createScope();

		AggregationKey<JsonObject> documentCountPerValue = AggregationKey.of( "documentCountPerValue" );

		SearchQuery<DocumentReference> query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.aggregation( documentCountPerValue, f -> f.fromJson(
						"{"
								+ "  'value_count' : {"
								+ "    'field' : 'nativeField_aggregation'"
								+ " }"
								+ "  }"
				) )
				.toQuery();
		JsonObject aggregationResult = query.fetchAll().aggregation( documentCountPerValue );
		assertJsonEquals(
				"{"
						+ "  'value': 3"
						+ "}",
				aggregationResult.toString()
		);
	}


	@Test
	public void aggregation_filter_fromLuceneQuery() {
		StubMappingScope scope = mainIndex.createScope();

		AggregationKey<Map<String, Long>> aggregationKey = AggregationKey.of( "agg" );

		SearchQuery<DocumentReference> query = scope.query()
				.extension( ElasticsearchExtension.get() )
				.where( f -> f.matchAll() )
				.aggregation( aggregationKey, f -> f.terms()
						.field( mainIndex.binding().nestedObject.relativeFieldName + ".aggregation1", String.class )
						// The provided predicate factory should already be extended and offer Elasticsearch-specific extensions
						.filter( pf -> pf.fromJson(
								"{'match': {'"
										+ mainIndex.binding().nestedObject.relativeFieldName + ".discriminator"
										+ "': 'included'}}"
						) )
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
	public void backend_unwrap() {
		Backend backend = integration.backend();
		assertThat( backend.unwrap( ElasticsearchBackend.class ) )
				.isNotNull();
	}

	@Test
	public void backend_unwrap_error_unknownType() {
		Backend backend = integration.backend();

		assertThatThrownBy( () -> backend.unwrap( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid requested type for this backend: '" + String.class.getName() + "'",
						"Elasticsearch backends can only be unwrapped to '" + ElasticsearchBackend.class.getName() + "'"
				);
	}

	@Test
	public void backend_getClient() throws Exception {
		Backend backend = integration.backend();
		ElasticsearchBackend elasticsearchBackend = backend.unwrap( ElasticsearchBackend.class );
		RestClient restClient = elasticsearchBackend.client( RestClient.class );

		// Test that the client actually works
		Response response = restClient.performRequest( new Request( "GET", "/" ) );
		assertThat( response.getStatusLine().getStatusCode() ).isEqualTo( 200 );
	}

	@Test
	public void backend_getClient_error_invalidClass() {
		Backend backend = integration.backend();
		ElasticsearchBackend elasticsearchBackend = backend.unwrap( ElasticsearchBackend.class );

		assertThatThrownBy( () -> elasticsearchBackend.client( HttpAsyncClient.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid requested type for client",
						HttpAsyncClient.class.getName(),
						"The Elasticsearch low-level client can only be unwrapped to",
						RestClient.class.getName()
				);
	}

	@Test
	public void mainIndex_unwrap() {
		IndexManager mainIndexFromIntegration = integration.indexManager( mainIndex.name() );
		assertThat( mainIndexFromIntegration.unwrap( ElasticsearchIndexManager.class ) )
				.isNotNull();
	}

	@Test
	public void mainIndex_unwrap_error_unknownType() {
		IndexManager mainIndexFromIntegration = integration.indexManager( mainIndex.name() );

		assertThatThrownBy( () -> mainIndexFromIntegration.unwrap( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid requested type for this index manager: '" + String.class.getName() + "'",
						"Elasticsearch index managers can only be unwrapped to '"
								+ ElasticsearchIndexManager.class.getName() + "'"
				);
	}

	@Test
	public void jsonHitProjectionInsideNested() {
		assertThatThrownBy( () -> mainIndex.createScope().query()
				.select( f -> f.object( "nestedObject" ).from(
						f.extension( ElasticsearchExtension.get() ).jsonHit()
				).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:json-hit' cannot be nested in an object projection",
						"A JSON hit projection represents a root hit object and adding it as a part of the nested object projection might produce misleading results."
				);
	}

	@Test
	public void sourceProjectionInsideNested() {
		assertThatThrownBy( () -> mainIndex.createScope().query()
				.select( f -> f.object( "nestedObject" ).from(
						f.extension( ElasticsearchExtension.get() ).source()
				).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:source' cannot be nested in an object projection",
						"A source projection represents a root source object and adding it as a part of the nested object projection might produce misleading results."
				);
	}

	@Test
	public void explanationProjectionInsideNested() {
		assertThatThrownBy( () -> mainIndex.createScope().query()
				.select( f -> f.object( "nestedObject" ).from(
						f.extension( ElasticsearchExtension.get() ).explanation()
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
		mainIndex.bulkIndexer()
				.add( SECOND_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 2" );

					document.addValue( mainIndex.binding().nativeField_integer, new JsonPrimitive( 2 ) );
					document.addValue( mainIndex.binding().nativeField_integer_converted, new JsonPrimitive( 2 ) );
					document.addValue( mainIndex.binding().nativeField_unsupportedType, new JsonPrimitive( "42" ) );

					document.addValue( mainIndex.binding().nativeField_sort1, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort2, new JsonPrimitive( "a" ) );
					document.addValue( mainIndex.binding().nativeField_sort3, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort4, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort5, new JsonPrimitive( "a" ) );

					document.addValue( mainIndex.binding().nativeField_aggregation,
							new JsonPrimitive( "value-for-doc-1-and-2" ) );

					DocumentElement nestedObject1 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject1.addValue( mainIndex.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.sort1, "b" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.aggregation1, "one" );
					DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject2.addValue( mainIndex.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.sort1, "a" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.aggregation1, "fifty-one" );
				} )
				.add( FIRST_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 1" );

					document.addValue( mainIndex.binding().nativeField_string, new JsonPrimitive( "text 1" ) );

					document.addValue( mainIndex.binding().nativeField_sort1, new JsonPrimitive( "a" ) );
					document.addValue( mainIndex.binding().nativeField_sort2, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort3, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort4, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort5, new JsonPrimitive( "a" ) );

					document.addValue( mainIndex.binding().nativeField_aggregation,
							new JsonPrimitive( "value-for-doc-1-and-2" ) );

					DocumentElement nestedObject1 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject1.addValue( mainIndex.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.sort1, "a" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.aggregation1, "two" );
					DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject2.addValue( mainIndex.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.sort1, "b" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.aggregation1, "fifty-two" );
				} )
				.add( THIRD_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 3" );

					document.addValue( mainIndex.binding().nativeField_geoPoint,
							gson.fromJson( "{'lat': 40.12, 'lon': -71.34}", JsonObject.class ) );

					document.addValue( mainIndex.binding().nativeField_sort1, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort2, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort3, new JsonPrimitive( "a" ) );
					document.addValue( mainIndex.binding().nativeField_sort4, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort5, new JsonPrimitive( "a" ) );

					document.addValue( mainIndex.binding().nativeField_aggregation, new JsonPrimitive( "value-for-doc-3" ) );

					DocumentElement nestedObject1 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject1.addValue( mainIndex.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.sort1, "c" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.aggregation1, "three" );
					DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject2.addValue( mainIndex.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.sort1, "b" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.aggregation1, "fifty-three" );
				} )
				.add( FOURTH_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 4" );

					document.addValue( mainIndex.binding().nativeField_dateWithColons, new JsonPrimitive( "2018:01:12" ) );

					document.addValue( mainIndex.binding().nativeField_sort1, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort2, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort3, new JsonPrimitive( "z" ) );
					document.addValue( mainIndex.binding().nativeField_sort4, new JsonPrimitive( "a" ) );
					document.addValue( mainIndex.binding().nativeField_sort5, new JsonPrimitive( "a" ) );

					DocumentElement nestedObject1 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject1.addValue( mainIndex.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.sort1, "d" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.aggregation1, "four" );
					DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject2.addValue( mainIndex.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.sort1, "c" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.aggregation1, "fifty-four" );
				} )
				.add( FIFTH_ID, document -> {
					document.addValue( mainIndex.binding().string, "text 5" );

					// This document should not match any query
					document.addValue( mainIndex.binding().nativeField_string, new JsonPrimitive( "text 2" ) );
					document.addValue( mainIndex.binding().nativeField_integer, new JsonPrimitive( 1 ) );
					document.addValue( mainIndex.binding().nativeField_geoPoint,
							gson.fromJson( "{'lat': 45.12, 'lon': -75.34}", JsonObject.class ) );
					document.addValue( mainIndex.binding().nativeField_dateWithColons, new JsonPrimitive( "2018:01:25" ) );
					document.addValue( mainIndex.binding().nativeField_unsupportedType, new JsonPrimitive( "foobar" ) ); // ignore_malformed is enabled, this should be ignored

					document.addValue( mainIndex.binding().nativeField_sort5, new JsonPrimitive( "z" ) );

					DocumentElement nestedObject1 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject1.addValue( mainIndex.binding().nestedObject.discriminator, "included" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.sort1, "e" );
					nestedObject1.addValue( mainIndex.binding().nestedObject.aggregation1, "five" );
					DocumentElement nestedObject2 = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject2.addValue( mainIndex.binding().nestedObject.discriminator, "excluded" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.sort1, "a" );
					nestedObject2.addValue( mainIndex.binding().nestedObject.aggregation1, "fifty-five" );
				} )
				.add( EMPTY_ID, document -> {} )
				.join();
	}

	private static class IndexBinding {
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

		IndexBinding(IndexSchemaElement root) {
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
							.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() )
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

			nestedObject = ObjectMapping.create( root, "nestedObject", ObjectStructure.NESTED, true );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final IndexFieldReference<String> discriminator;
		final IndexFieldReference<String> sort1;
		final IndexFieldReference<String> aggregation1;

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
		}
	}
}
