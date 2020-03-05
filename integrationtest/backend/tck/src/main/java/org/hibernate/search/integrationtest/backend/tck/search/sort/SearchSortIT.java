/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.spi.DelegatingSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.assertj.core.api.Assertions;

/**
 * Generic tests for sorts. More specific tests can be found in other classes, such as {@link FieldSearchSortIT}.
 */
public class SearchSortIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String ANOTHER_INDEX_NAME = "AnotherIndexName";

	private static final int INDEX_ORDER_CHECKS = 10;

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String EMPTY_ID = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private StubMappingIndexManager anotherIndexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						ANOTHER_INDEX_NAME,
						// Using the same mapping here. But a different mapping would work the same.
						// What matters here is that is a different index.
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.anotherIndexManager = indexManager
				)
				.setup();

		initData();
	}

	private SearchQuery<DocumentReference> simpleQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	@Test
	public void byIndexOrder() {
		/*
		 * We don't really know in advance what the index order is, but we want it to be consistent.
		 * Thus we just test that the order stays the same over several calls.
		 */

		SearchQuery<DocumentReference> query = simpleQuery( b -> b.indexOrder() );
		SearchResult<DocumentReference> firstCallResult = query.fetchAll();
		assertThat( firstCallResult ).fromQuery( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		List<DocumentReference> firstCallHits = firstCallResult.getHits();

		for ( int i = 0; i < INDEX_ORDER_CHECKS; ++i ) {
			// Rebuild the query to bypass any cache in the query object
			query = simpleQuery( b -> b.indexOrder() );
			assertThat( query ).hasHitsExactOrder( firstCallHits );
		}
	}

	@Test
	public void byField_flattened() {
		SearchQuery<DocumentReference> query;
		query = simpleQuery( b -> b.field( "flattenedObject.string" )
				.asc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "flattenedObject.string" )
				.desc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "flattenedObject.integer" )
				.asc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "flattenedObject.integer" )
				.desc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-2254" )
	public void byField_nested() {
		SearchQuery<DocumentReference> query;
		query = simpleQuery( b -> b.field( "nestedObject.string" )
				.asc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "nestedObject.string" )
				.desc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "nestedObject.string" )
				.asc().missing().first() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, FIRST_ID, SECOND_ID, THIRD_ID );

		query = simpleQuery( b -> b.field( "nestedObject.string" )
				.desc().missing().first() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, THIRD_ID, SECOND_ID, FIRST_ID );

		query = simpleQuery( b -> b.field( "nestedObject.integer" )
				.asc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "nestedObject.integer" )
				.desc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "nestedObject.integer" )
				.asc().missing().first() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, FIRST_ID, SECOND_ID, THIRD_ID );

		query = simpleQuery( b -> b.field( "nestedObject.integer" )
				.desc().missing().first() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, THIRD_ID, SECOND_ID, FIRST_ID );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-2254" )
	public void byField_nested_x2() {
		SearchQuery<DocumentReference> query;
		query = simpleQuery( b -> b.field( "nestedObject.nestedObject.string" )
				.asc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "nestedObject.nestedObject.string" )
				.desc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "nestedObject.nestedObject.string" )
				.asc().missing().first() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, FIRST_ID, SECOND_ID, THIRD_ID );

		query = simpleQuery( b -> b.field( "nestedObject.nestedObject.string" )
				.desc().missing().first() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, THIRD_ID, SECOND_ID, FIRST_ID );

		query = simpleQuery( b -> b.field( "nestedObject.nestedObject.integer" )
				.asc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "nestedObject.nestedObject.integer" )
				.desc().missing().last() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		query = simpleQuery( b -> b.field( "nestedObject.nestedObject.integer" )
				.asc().missing().first() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, FIRST_ID, SECOND_ID, THIRD_ID );

		query = simpleQuery( b -> b.field( "nestedObject.nestedObject.integer" )
				.desc().missing().first() );
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, THIRD_ID, SECOND_ID, FIRST_ID );
	}

	@Test
	public void byScore() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query;

		SearchPredicate predicate = scope.predicate()
				.match().field( "string_analyzed_forScore" ).matching( "hooray" ).toPredicate();

		query = scope.query()
				.where( predicate )
				.sort( f -> f.score() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID );

		query = scope.query()
				.where( predicate )
				.sort( f -> f.score().desc() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID );

		query = scope.query()
				.where( predicate )
				.sort( f -> f.score().asc() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID );
	}

	@Test
	public void separateSort() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query;

		SearchSort sortAsc = scope.sort()
				.field( "string" ).asc().missing().last()
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sortAsc )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		SearchSort sortDesc = scope.sort()
				.field( "string" ).desc().missing().last()
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sortDesc )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
	}

	@Test
	public void reuseSortInstance_onScopeTargetingSameIndexes() {
		StubMappingScope scope = indexManager.createScope();
		SearchSort sort = scope
				.sort().field( "string" ).asc().missing().last().toSort();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		// reuse the same sort instance on the same scope
		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		// reuse the same sort instance on a different scope,
		// targeting the same index
		query = indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		sort = indexManager.createScope( anotherIndexManager )
				.sort().field( "string" ).asc().missing().last().toSort();

		// reuse the same sort instance on a different scope,
		// targeting same indexes
		query = anotherIndexManager.createScope( indexManager ).query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	@Test
	public void reuseSortInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = indexManager.createScope();
		SearchSort sort = scope
				.sort().field( "string" ).asc().missing().last().toSort();

		// reuse the same sort instance on a different scope,
		// targeting a different index
		SubTest.expectException( () ->
				anotherIndexManager.createScope().query()
						.where( f -> f.matchAll() )
						.sort( sort )
						.toQuery() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( ANOTHER_INDEX_NAME );

		// reuse the same sort instance on a different scope,
		// targeting different indexes
		SubTest.expectException( () ->
				indexManager.createScope( anotherIndexManager ).query()
						.where( f -> f.matchAll() )
						.sort( sort )
						.toQuery() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( ANOTHER_INDEX_NAME );
	}

	@Test
	public void byDistance_asc() {
		SearchQuery<DocumentReference> query = simpleQuery( b -> b.distance( "geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.distance( "geoPoint", 45.757864, 4.834496 ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.distance( "geoPoint", 45.757864, 4.834496 ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );
	}

	@Test
	public void byDistance_desc() {
		SearchQuery<DocumentReference> query = simpleQuery(
				b -> b.distance( "geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );

		query = simpleQuery( b -> b.distance( "geoPoint", 45.757864, 4.834496 ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );
	}

	@Test
	public void distanceSort_invalidType() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Distance related operations are not supported" );
		thrown.expectMessage( "string" );

		simpleQuery(
				b -> b.distance( "string", GeoPoint.of( 45.757864, 4.834496 ) ).desc()
		);
	}

	@Test
	public void byDistance_asc_flattened() {
		SearchQuery<DocumentReference> query = simpleQuery( b -> b.distance( "flattenedObject.geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.distance( "flattenedObject.geoPoint", 45.757864, 4.834496 ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.distance( "flattenedObject.geoPoint", 45.757864, 4.834496 ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );
	}

	@Test
	public void byDistance_desc_flattened() {
		SearchQuery<DocumentReference> query = simpleQuery(
				b -> b.distance( "flattenedObject.geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );

		query = simpleQuery( b -> b.distance( "flattenedObject.geoPoint", 45.757864, 4.834496 ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );
	}

	@Test
	public void byDistance_asc_nested() {
		SearchQuery<DocumentReference> query = simpleQuery( b -> b.distance( "nestedObject.geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.distance( "nestedObject.geoPoint", 45.757864, 4.834496 ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.distance( "nestedObject.geoPoint", 45.757864, 4.834496 ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );
	}

	@Test
	public void byDistance_desc_nested() {
		SearchQuery<DocumentReference> query = simpleQuery(
				b -> b.distance( "nestedObject.geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );

		query = simpleQuery( b -> b.distance( "nestedObject.geoPoint", 45.757864, 4.834496 ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );
	}

	@Test
	public void byDistance_asc_nested_x2() {
		SearchQuery<DocumentReference> query = simpleQuery( b -> b.distance( "nestedObject.nestedObject.geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.distance( "nestedObject.nestedObject.geoPoint", 45.757864, 4.834496 ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.distance( "nestedObject.nestedObject.geoPoint", 45.757864, 4.834496 ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );
	}

	@Test
	public void byDistance_desc_nested_x2() {
		SearchQuery<DocumentReference> query = simpleQuery(
				b -> b.distance( "nestedObject.nestedObject.geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );

		query = simpleQuery( b -> b.distance( "nestedObject.nestedObject.geoPoint", 45.757864, 4.834496 ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );
	}

	@Test
	public void extension() {
		SearchQuery<DocumentReference> query;

		// Mandatory extension, supported
		query = simpleQuery( c -> c
				.extension( new SupportedExtension() ).field( "string" ).missing().last()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension( new SupportedExtension() ).field( "string" ).desc().missing().last()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Mandatory extension, unsupported
		SubTest.expectException(
				() -> indexManager.createScope().sort().extension( new UnSupportedExtension() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );

		// Conditional extensions with orElse - two, both supported
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new SupportedExtension(),
								c -> c.field( "string" ).missing().last()
						)
						.ifSupported(
								new SupportedExtension(),
								ignored -> Assertions.fail( "This should not be called" )
						)
						.orElseFail()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new SupportedExtension(),
								c -> c.field( "string" ).desc().missing().last()
						)
						.ifSupported(
								new SupportedExtension(),
								ignored -> Assertions.fail( "This should not be called" )
						)
						.orElseFail()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Conditional extensions with orElse - two, second supported
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assertions.fail( "This should not be called" )
						)
						.ifSupported(
								new SupportedExtension(),
								c -> c.field( "string" ).missing().last()
						)
						.orElse( ignored -> Assertions.fail( "This should not be called" ) )
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assertions.fail( "This should not be called" )
						)
						.ifSupported(
								new SupportedExtension(),
								c -> c.field( "string" ).desc().missing().last()
						)
						.orElse( ignored -> Assertions.fail( "This should not be called" ) )
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Conditional extensions with orElse - two, both unsupported
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assertions.fail( "This should not be called" )
						)
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assertions.fail( "This should not be called" )
						)
						.orElse(
								c -> c.field( "string" ).missing().last()
						)
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assertions.fail( "This should not be called" )
						)
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assertions.fail( "This should not be called" )
						)
						.orElse(
								c -> c.field( "string" ).desc().missing().last()
						)
		);
	}

	private void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		// Important: do not index the documents in the expected order after sorts
		plan.add( referenceProvider( SECOND_ID ), document -> {
			GeoPoint geoPoint = GeoPoint.of( 45.7705687, 4.835233 );

			document.addValue( indexMapping.string, "george" );
			document.addValue( indexMapping.geoPoint, geoPoint );

			document.addValue( indexMapping.string_analyzed_forScore, "Hooray Hooray" );
			document.addValue( indexMapping.unsortable, "george" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "george" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 2 );
			flattenedObject.addValue( indexMapping.flattenedObject.geoPoint, geoPoint );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "george" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 2 );
			nestedObject.addValue( indexMapping.nestedObject.geoPoint, geoPoint );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedX2Object = nestedObject.addObject( indexMapping.nestedX2Object.self );
			nestedX2Object.addValue( indexMapping.nestedX2Object.string, "george" );
			nestedX2Object.addValue( indexMapping.nestedX2Object.integer, 2 );
			nestedX2Object.addValue( indexMapping.nestedX2Object.geoPoint, geoPoint );
		} );
		plan.add( referenceProvider( FIRST_ID ), document -> {
			GeoPoint geoPoint = GeoPoint.of( 45.7541719, 4.8386221 );

			document.addValue( indexMapping.string, "aaron" );
			document.addValue( indexMapping.geoPoint, geoPoint );

			document.addValue( indexMapping.string_analyzed_forScore, "Hooray Hooray Hooray" );
			document.addValue( indexMapping.unsortable, "aaron" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "aaron" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 1 );
			flattenedObject.addValue( indexMapping.flattenedObject.geoPoint, geoPoint );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "aaron" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 1 );
			nestedObject.addValue( indexMapping.nestedObject.geoPoint, geoPoint );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedX2Object = nestedObject.addObject( indexMapping.nestedX2Object.self );
			nestedX2Object.addValue( indexMapping.nestedX2Object.string, "aaron" );
			nestedX2Object.addValue( indexMapping.nestedX2Object.integer, 1 );
			nestedX2Object.addValue( indexMapping.nestedX2Object.geoPoint, geoPoint );
		} );
		plan.add( referenceProvider( THIRD_ID ), document -> {
			GeoPoint geoPoint = GeoPoint.of( 45.7530374, 4.8510299 );

			document.addValue( indexMapping.string, "zach" );
			document.addValue( indexMapping.geoPoint, geoPoint );

			document.addValue( indexMapping.string_analyzed_forScore, "Hooray" );
			document.addValue( indexMapping.unsortable, "zach" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "zach" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 3 );
			flattenedObject.addValue( indexMapping.flattenedObject.geoPoint, geoPoint );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "zach" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 3 );
			nestedObject.addValue( indexMapping.nestedObject.geoPoint, geoPoint );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedX2Object = nestedObject.addObject( indexMapping.nestedX2Object.self );
			nestedX2Object.addValue( indexMapping.nestedX2Object.string, "zach" );
			nestedX2Object.addValue( indexMapping.nestedX2Object.integer, 3 );
			nestedX2Object.addValue( indexMapping.nestedX2Object.geoPoint, geoPoint );
		} );
		plan.add( referenceProvider( EMPTY_ID ), document -> { } );

		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;
		final IndexFieldReference<GeoPoint> geoPoint;
		final IndexFieldReference<String> string_analyzed_forScore;
		final IndexFieldReference<String> unsortable;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;
		final ObjectMapping nestedX2Object;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
			geoPoint = root.field( "geoPoint", f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.toReference();
			string_analyzed_forScore = root.field(
					"string_analyzed_forScore" ,
					f -> f.asString()
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
			unsortable = root.field( "unsortable", f -> f.asString().sortable( Sortable.NO ) )
					.toReference();

			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new ObjectMapping( flattenedObjectField );

			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new ObjectMapping( nestedObjectField );

			IndexSchemaObjectField nestedX2ObjectField = nestedObjectField.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedX2Object = new ObjectMapping( nestedX2ObjectField );
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;
		final IndexFieldReference<GeoPoint> geoPoint;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			string = objectField.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
			integer = objectField.field( "integer", f -> f.asInteger().sortable( Sortable.YES ) )
					.toReference();
			geoPoint = objectField.field( "geoPoint", f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.toReference();
		}
	}

	private static class SupportedExtension implements SearchSortFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchSortFactory original,
				SearchSortDslContext<?, ?> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			Assertions.assertThat( dslContext.getBuilderFactory() ).isNotNull();
			return Optional.of( new MyExtendedFactory( original ) );
		}
	}

	private static class UnSupportedExtension implements SearchSortFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchSortFactory original,
				SearchSortDslContext<?, ?> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			Assertions.assertThat( dslContext.getBuilderFactory() ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedFactory extends DelegatingSearchSortFactory {
		MyExtendedFactory(SearchSortFactory delegate) {
			super( delegate );
		}
	}
}
