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
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.spi.DelegatingSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

/**
 * Generic tests for sorts. More specific tests can be found in other classes,
 * such as {@link FieldSearchSortBaseIT} or {@link DistanceSearchSortBaseIT}.
 */
public class SearchSortIT {

	private static final int INDEX_ORDER_CHECKS = 10;

	private static final String FIRST_ID = "1";
	private static final String SECOND_ID = "2";
	private static final String THIRD_ID = "3";
	private static final String EMPTY_ID = "empty";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<IndexBinding> otherIndex =
			// Using the same mapping here. But a different mapping would work the same.
			// What matters here is that is a different index.
			SimpleMappedIndex.of( IndexBinding::new ).name( "other" );

	@Before
	public void setup() {
		setupHelper.start().withIndexes( mainIndex, otherIndex ).setup();

		initData();
	}

	private SearchQuery<DocumentReference> simpleQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		StubMappingScope scope = mainIndex.createScope();
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
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		List<DocumentReference> firstCallHits = firstCallResult.getHits();

		for ( int i = 0; i < INDEX_ORDER_CHECKS; ++i ) {
			// Rebuild the query to bypass any cache in the query object
			query = simpleQuery( b -> b.indexOrder() );
			assertThat( query ).hasHitsExactOrder( firstCallHits );
		}
	}

	@Test
	public void byScore() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query;

		SearchPredicate predicate = scope.predicate()
				.match().field( "string_analyzed_forScore" ).matching( "hooray" ).toPredicate();

		query = scope.query()
				.where( predicate )
				.sort( f -> f.score() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID );

		query = scope.query()
				.where( predicate )
				.sort( f -> f.score().desc() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID );

		query = scope.query()
				.where( predicate )
				.sort( f -> f.score().asc() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID );
	}

	@Test
	public void separateSort() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query;

		SearchSort sortAsc = scope.sort()
				.field( "string" ).asc().missing().last()
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sortAsc )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		SearchSort sortDesc = scope.sort()
				.field( "string" ).desc().missing().last()
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sortDesc )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
	}

	@Test
	public void reuseSortInstance_onScopeTargetingSameIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		SearchSort sort = scope
				.sort().field( "string" ).asc().missing().last().toSort();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		// reuse the same sort instance on the same scope
		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		// reuse the same sort instance on a different scope,
		// targeting the same index
		query = mainIndex.createScope().query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		sort = mainIndex.createScope( otherIndex )
				.sort().field( "string" ).asc().missing().last().toSort();

		// reuse the same sort instance on a different scope,
		// targeting same indexes
		query = otherIndex.createScope( mainIndex ).query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThat( query ).hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	@Test
	public void reuseSortInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		SearchSort sort = scope
				.sort().field( "string" ).asc().missing().last().toSort();

		// reuse the same sort instance on a different scope,
		// targeting a different index
		Assertions.assertThatThrownBy( () ->
				otherIndex.createScope().query()
						.where( f -> f.matchAll() )
						.sort( sort )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( mainIndex.name() )
				.hasMessageContaining( otherIndex.name() );

		// reuse the same sort instance on a different scope,
		// targeting different indexes
		Assertions.assertThatThrownBy( () ->
				mainIndex.createScope( otherIndex ).query()
						.where( f -> f.matchAll() )
						.sort( sort )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( mainIndex.name() )
				.hasMessageContaining( otherIndex.name() );
	}

	@Test
	public void extension() {
		SearchQuery<DocumentReference> query;

		// Mandatory extension, supported
		query = simpleQuery( c -> c
				.extension( new SupportedExtension() ).field( "string" ).missing().last()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension( new SupportedExtension() ).field( "string" ).desc().missing().last()
		);
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Mandatory extension, unsupported
		Assertions.assertThatThrownBy(
				() -> mainIndex.createScope().sort().extension( new UnSupportedExtension() )
		)
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
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
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
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

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
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
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
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

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
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
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
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = mainIndex.createIndexingPlan();
		// Important: do not index the documents in the expected order after sorts
		plan.add( referenceProvider( SECOND_ID ), document -> {
			document.addValue( mainIndex.binding().string, "george" );
			document.addValue( mainIndex.binding().string_analyzed_forScore, "Hooray Hooray" );
			document.addValue( mainIndex.binding().unsortable, "george" );
		} );
		plan.add( referenceProvider( FIRST_ID ), document -> {
			document.addValue( mainIndex.binding().string, "aaron" );
			document.addValue( mainIndex.binding().string_analyzed_forScore, "Hooray Hooray Hooray" );
			document.addValue( mainIndex.binding().unsortable, "aaron" );
		} );
		plan.add( referenceProvider( THIRD_ID ), document -> {
			document.addValue( mainIndex.binding().string, "zach" );
			document.addValue( mainIndex.binding().string_analyzed_forScore, "Hooray" );
			document.addValue( mainIndex.binding().unsortable, "zach" );
		} );
		plan.add( referenceProvider( EMPTY_ID ), document -> { } );

		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> string_analyzed_forScore;
		final IndexFieldReference<String> unsortable;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
			string_analyzed_forScore = root.field(
					"string_analyzed_forScore" ,
					f -> f.asString()
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
			unsortable = root.field( "unsortable", f -> f.asString().sortable( Sortable.NO ) )
					.toReference();
		}
	}

	private static class SupportedExtension implements SearchSortFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchSortFactory original,
				SearchSortDslContext<?, ?, ?> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			Assertions.assertThat( dslContext.getBuilderFactory() ).isNotNull();
			return Optional.of( new MyExtendedFactory( original, dslContext ) );
		}
	}

	private static class UnSupportedExtension implements SearchSortFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchSortFactory original,
				SearchSortDslContext<?, ?, ?> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			Assertions.assertThat( dslContext.getBuilderFactory() ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedFactory extends DelegatingSearchSortFactory<SearchPredicateFactory> {
		MyExtendedFactory(SearchSortFactory delegate, SearchSortDslContext<?, ?, ?> dslContext) {
			super( delegate, dslContext );
		}
	}
}
