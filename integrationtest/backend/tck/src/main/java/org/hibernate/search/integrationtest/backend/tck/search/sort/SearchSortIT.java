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
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
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
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;

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

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext> sortContributor) {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		return searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( sortContributor )
				.build();
	}

	@Test
	public void byIndexOrder() {
		/*
		 * We don't really know in advance what the index order is, but we want it to be consistent.
		 * Thus we just test that the order stays the same over several calls.
		 */

		SearchQuery<DocumentReference> query = simpleQuery( b -> b.byIndexOrder() );
		SearchResult<DocumentReference> firstCallResult = query.execute();
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query;

		SearchPredicate predicate = searchTarget.predicate()
				.match().onField( "string_analyzed_forScore" ).matching( "hooray" ).toPredicate();

		query = searchTarget.query()
				.asReference()
				.predicate( predicate )
				.sort( c -> c.byScore() )
				.build();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( predicate )
				.sort( c -> c.byScore().desc() )
				.build();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( predicate )
				.sort( c -> c.byScore().asc() )
				.build();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID );
	}

	@Test
	public void separateSort() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query;

		SearchSort sortAsc = searchTarget.sort()
				.byField( "string" ).asc().onMissingValue().sortLast()
				.toSort();

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( sortAsc )
				.build();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sortAsc ) )
				.build();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		SearchSort sortDesc = searchTarget.sort()
				.byField( "string" ).desc().onMissingValue().sortLast()
				.toSort();

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( sortDesc )
				.build();
		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.by( sortDesc ) )
				.build();
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

		// Mandatory extension
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
			indexAccessors.string.write( document, "george" );
			indexAccessors.geoPoint.write( document, GeoPoint.of( 45.7705687,4.835233 ) );

			indexAccessors.string_analyzed_forScore.write( document, "Hooray Hooray" );
			indexAccessors.unsortable.write( document, "george" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.flattenedObject.string.write( flattenedObject, "george" );
			indexAccessors.flattenedObject.integer.write( flattenedObject, 2 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, "george" );
			indexAccessors.nestedObject.integer.write( nestedObject, 2 );
		} );
		workPlan.add( referenceProvider( FIRST_ID ), document -> {
			indexAccessors.string.write( document, "aaron" );
			indexAccessors.geoPoint.write( document, GeoPoint.of( 45.7541719, 4.8386221 ) );

			indexAccessors.string_analyzed_forScore.write( document, "Hooray Hooray Hooray" );
			indexAccessors.unsortable.write( document, "aaron" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.flattenedObject.string.write( flattenedObject, "aaron" );
			indexAccessors.flattenedObject.integer.write( flattenedObject, 1 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, "aaron" );
			indexAccessors.nestedObject.integer.write( nestedObject, 1 );
		} );
		workPlan.add( referenceProvider( THIRD_ID ), document -> {
			indexAccessors.string.write( document, "zach" );
			indexAccessors.geoPoint.write( document, GeoPoint.of( 45.7530374, 4.8510299 ) );

			indexAccessors.string_analyzed_forScore.write( document, "Hooray" );
			indexAccessors.unsortable.write( document, "zach" );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.flattenedObject.string.write( flattenedObject, "zach" );
			indexAccessors.flattenedObject.integer.write( flattenedObject, 3 );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.string.write( nestedObject, "zach" );
			indexAccessors.nestedObject.integer.write( nestedObject, 3 );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<GeoPoint> geoPoint;
		final IndexFieldAccessor<String> string_analyzed_forScore;
		final IndexFieldAccessor<String> unsortable;

		final ObjectAccessors flattenedObject;
		final ObjectAccessors nestedObject;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.createAccessor();
			geoPoint = root.field( "geoPoint", f -> f.asGeoPoint().sortable( Sortable.YES ) )
					.createAccessor();
			string_analyzed_forScore = root.field(
					"string_analyzed_forScore" ,
					f -> f.asString()
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
			)
					.createAccessor();
			unsortable = root.field( "unsortable", f -> f.asString().sortable( Sortable.NO ) )
					.createAccessor();

			IndexSchemaObjectField flattenedObjectField =
					root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new ObjectAccessors( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField =
					root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new ObjectAccessors( nestedObjectField );
		}
	}

	private static class ObjectAccessors {
		final IndexObjectFieldAccessor self;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<String> string;

		ObjectAccessors(IndexSchemaObjectField objectField) {
			self = objectField.createAccessor();
			string = objectField.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.createAccessor();
			integer = objectField.field( "integer", f -> f.asInteger().sortable( Sortable.YES ) )
					.createAccessor();
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
