/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.spi.DelegatingSearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assert;
import org.junit.Assume;
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

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext> sortContributor) {
		StubMappingSearchScope scope = indexManager.createSearchScope();
		return scope.query()
				.predicate( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	@Test
	public void byIndexOrder() {
		/*
		 * We don't really know in advance what the index order is, but we want it to be consistent.
		 * Thus we just test that the order stays the same over several calls.
		 */

		SearchQuery<DocumentReference> query = simpleQuery( b -> b.byIndexOrder() );
		SearchResult<DocumentReference> firstCallResult = query.fetch();
		assertThat( firstCallResult ).fromQuery( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		List<DocumentReference> firstCallHits = firstCallResult.getHits();

		for ( int i = 0; i < INDEX_ORDER_CHECKS; ++i ) {
			// Rebuild the query to bypass any cache in the query object
			query = simpleQuery( b -> b.byIndexOrder() );
			assertThat( query ).hasHitsExactOrder( firstCallHits );
		}
	}

	@Test
	public void nested() {
		Assume.assumeTrue( "Sorts on fields within nested fields are not supported yet", false );
		// TODO support sorts on fields within nested fields
	}

	@Test
	public void byScore() {
		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query;

		SearchPredicate predicate = scope.predicate()
				.match().onField( "string_analyzed_forScore" ).matching( "hooray" ).toPredicate();

		query = scope.query()
				.predicate( predicate )
				.sort( c -> c.byScore() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID );

		query = scope.query()
				.predicate( predicate )
				.sort( c -> c.byScore().desc() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID );

		query = scope.query()
				.predicate( predicate )
				.sort( c -> c.byScore().asc() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID );
	}

	@Test
	public void separateSort() {
		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query;

		SearchSort sortAsc = scope.sort()
				.byField( "string" ).asc().onMissingValue().sortLast()
				.toSort();

		query = scope.query()
				.predicate( f -> f.matchAll() )
				.sort( sortAsc )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sortAsc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		SearchSort sortDesc = scope.sort()
				.byField( "string" ).desc().onMissingValue().sortLast()
				.toSort();

		query = scope.query()
				.predicate( f -> f.matchAll() )
				.sort( sortDesc )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		query = scope.query()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sortDesc ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
	}

	@Test
	public void lambda_caching() {
		AtomicReference<SearchSort> cache = new AtomicReference<>();

		Consumer<? super SearchSortContainerContext> cachingContributor = c -> {
			if ( cache.get() == null ) {
				SearchSort result = c.byField( "string" ).onMissingValue().sortLast().toSort();
				cache.set( result );
			}
			else {
				c.by( cache.get() );
			}
		};

		Assertions.assertThat( cache ).hasValue( null );

		SearchQuery<DocumentReference> query;

		query = simpleQuery( cachingContributor );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		Assertions.assertThat( cache ).doesNotHaveValue( null );

		query = simpleQuery( cachingContributor );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	@Test
	public void byDistance_asc() {
		SearchQuery<DocumentReference> query = simpleQuery( b -> b.byDistance( "geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.byDistance( "geoPoint", 45.757864, 4.834496 ) );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );

		query = simpleQuery( b -> b.byDistance( "geoPoint", 45.757864, 4.834496 ).asc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, THIRD_ID, SECOND_ID, EMPTY_ID );
	}

	@Test
	public void byDistance_desc() {
		Assume.assumeTrue(
				"Descending distance sort is not supported, skipping.",
				TckConfiguration.get().getBackendFeatures().distanceSortDesc()
		);

		SearchQuery<DocumentReference> query = simpleQuery(
				b -> b.byDistance( "geoPoint", GeoPoint.of( 45.757864, 4.834496 ) ).desc()
		);
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );

		query = simpleQuery( b -> b.byDistance( "geoPoint", 45.757864, 4.834496 ).desc() );
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, EMPTY_ID, SECOND_ID, THIRD_ID, FIRST_ID );
	}

	@Test
	public void distanceSort_invalidType() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Distance related operations are not supported" );
		thrown.expectMessage( "string" );

		simpleQuery(
				b -> b.byDistance( "string", GeoPoint.of( 45.757864, 4.834496 ) ).desc()
		);
	}

	@Test
	public void extension() {
		SearchQuery<DocumentReference> query;

		// Mandatory extension, supported
		query = simpleQuery( c -> c
				.extension( new SupportedExtension() ).byField( "string" ).onMissingValue().sortLast()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension( new SupportedExtension() ).byField( "string" ).desc().onMissingValue().sortLast()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Mandatory extension, unsupported
		SubTest.expectException(
				() -> indexManager.createSearchScope().sort().extension( new UnSupportedExtension() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );

		// Conditional extensions with orElse - two, both supported
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new SupportedExtension(),
								c -> c.byField( "string" ).onMissingValue().sortLast()
						)
						.ifSupported(
								new SupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
						)
						.orElseFail()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new SupportedExtension(),
								c -> c.byField( "string" ).desc().onMissingValue().sortLast()
						)
						.ifSupported(
								new SupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
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
								ignored -> Assert.fail( "This should not be called" )
						)
						.ifSupported(
								new SupportedExtension(),
								c -> c.byField( "string" ).onMissingValue().sortLast()
						)
						.orElse( ignored -> Assert.fail( "This should not be called" ) )
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
						)
						.ifSupported(
								new SupportedExtension(),
								c -> c.byField( "string" ).desc().onMissingValue().sortLast()
						)
						.orElse( ignored -> Assert.fail( "This should not be called" ) )
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Conditional extensions with orElse - two, both unsupported
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
						)
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
						)
						.orElse(
								c -> c.byField( "string" ).onMissingValue().sortLast()
						)
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
						)
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
						)
						.orElse(
								c -> c.byField( "string" ).desc().onMissingValue().sortLast()
						)
		);

		// Conditional extensions without orElse - one, unsupported
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
		query = simpleQuery( b -> {
			b.extension()
					.ifSupported(
							new UnSupportedExtension(),
							ignored -> Assert.fail( "This should not be called" )
					);
			b.byField( "string" ).onMissingValue().sortLast();
		} );
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> {
			b.extension()
					.ifSupported(
							new UnSupportedExtension(),
							ignored -> Assert.fail( "This should not be called" )
					);
			b.byField( "string" ).desc().onMissingValue().sortLast();
		} );
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		// Important: do not index the documents in the expected order after sorts
		workPlan.add( referenceProvider( SECOND_ID ), document -> {
			document.addValue( indexMapping.string, "george" );
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 45.7705687,4.835233 ) );

			document.addValue( indexMapping.string_analyzed_forScore, "Hooray Hooray" );
			document.addValue( indexMapping.unsortable, "george" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "george" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 2 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "george" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 2 );
		} );
		workPlan.add( referenceProvider( FIRST_ID ), document -> {
			document.addValue( indexMapping.string, "aaron" );
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 45.7541719, 4.8386221 ) );

			document.addValue( indexMapping.string_analyzed_forScore, "Hooray Hooray Hooray" );
			document.addValue( indexMapping.unsortable, "aaron" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "aaron" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 1 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "aaron" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 1 );
		} );
		workPlan.add( referenceProvider( THIRD_ID ), document -> {
			document.addValue( indexMapping.string, "zach" );
			document.addValue( indexMapping.geoPoint, GeoPoint.of( 45.7530374, 4.8510299 ) );

			document.addValue( indexMapping.string_analyzed_forScore, "Hooray" );
			document.addValue( indexMapping.unsortable, "zach" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			flattenedObject.addValue( indexMapping.flattenedObject.string, "zach" );
			flattenedObject.addValue( indexMapping.flattenedObject.integer, 3 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			nestedObject.addValue( indexMapping.nestedObject.string, "zach" );
			nestedObject.addValue( indexMapping.nestedObject.integer, 3 );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
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

			IndexSchemaObjectField flattenedObjectField =
					root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new ObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new ObjectMapping( nestedObjectField );
		}
	}

	private static class ObjectMapping {
		final IndexObjectFieldReference self;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;

		ObjectMapping(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			string = objectField.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
			integer = objectField.field( "integer", f -> f.asInteger().sortable( Sortable.YES ) )
					.toReference();
		}
	}

	private static class SupportedExtension implements SearchSortContainerContextExtension<MyExtendedContext> {
		@Override
		public <C, B> Optional<MyExtendedContext> extendOptional(SearchSortContainerContext original,
				SearchSortBuilderFactory<C, B> factory, SearchSortDslContext<? super B> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			return Optional.of( new MyExtendedContext( original ) );
		}
	}

	private static class UnSupportedExtension implements SearchSortContainerContextExtension<MyExtendedContext> {
		@Override
		public <C, B> Optional<MyExtendedContext> extendOptional(SearchSortContainerContext original,
				SearchSortBuilderFactory<C, B> factory, SearchSortDslContext<? super B> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedContext extends DelegatingSearchSortContainerContext {
		MyExtendedContext(SearchSortContainerContext delegate) {
			super( delegate );
		}
	}
}
