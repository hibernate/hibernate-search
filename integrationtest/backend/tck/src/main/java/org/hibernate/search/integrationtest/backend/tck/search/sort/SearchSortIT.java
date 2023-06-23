/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Generic tests for sorts. More specific tests can be found in other classes,
 * such as {@link FieldSortBaseIT} or {@link DistanceSortBaseIT}.
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
		assertThatResult( firstCallResult ).fromQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		List<DocumentReference> firstCallHits = firstCallResult.hits();

		for ( int i = 0; i < INDEX_ORDER_CHECKS; ++i ) {
			// Rebuild the query to bypass any cache in the query object
			query = simpleQuery( b -> b.indexOrder() );
			assertThatQuery( query ).hasHitsExactOrder( firstCallHits );
		}
	}

	@Test
	public void byDefault_score() {
		StubMappingScope scope = mainIndex.createScope();

		assertThatQuery( scope.query()
				.where( f -> f.match().field( "string_analyzed_forScore" ).matching( "hooray" ) )
				.toQuery() )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID );

		assertThatQuery( scope.query()
				.where( f -> f.match().field( "string_analyzed_forScore_reversed" ).matching( "hooray" ) )
				.toQuery() )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID );
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
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID );

		query = scope.query()
				.where( predicate )
				.sort( f -> f.score().desc() )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID );

		query = scope.query()
				.where( predicate )
				.sort( f -> f.score().asc() )
				.toQuery();
		assertThatQuery( query )
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
		assertThatQuery( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		SearchSort sortDesc = scope.sort()
				.field( "string" ).desc().missing().last()
				.toSort();

		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sortDesc )
				.toQuery();
		assertThatQuery( query )
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

		assertThatQuery( query ).hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		// reuse the same sort instance on the same scope
		query = scope.query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		// reuse the same sort instance on a different scope,
		// targeting the same index
		query = mainIndex.createScope().query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );

		sort = mainIndex.createScope( otherIndex )
				.sort().field( "string" ).asc().missing().last().toSort();

		// reuse the same sort instance on a different scope,
		// targeting same indexes
		query = otherIndex.createScope( mainIndex ).query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery();

		assertThatQuery( query ).hasDocRefHitsExactOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
	}

	@Test
	public void reuseSortInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		SearchSort sort = scope
				.sort().field( "string" ).asc().missing().last().toSort();

		// reuse the same sort instance on a different scope,
		// targeting a different index
		assertThatThrownBy( () -> otherIndex.createScope().query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search sort",
						"You must build the sort from a scope targeting indexes ", otherIndex.name(),
						"the given sort was built from a scope targeting ", mainIndex.name() );

		// reuse the same sort instance on a different scope,
		// targeting different indexes
		assertThatThrownBy( () -> mainIndex.createScope( otherIndex ).query()
				.where( f -> f.matchAll() )
				.sort( sort )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search sort",
						"You must build the sort from a scope targeting indexes ",
						mainIndex.name(), otherIndex.name(),
						"the given sort was built from a scope targeting ", mainIndex.name() );
	}

	@Test
	public void extension() {
		SearchQuery<DocumentReference> query;

		// Mandatory extension, supported
		query = simpleQuery( c -> c
				.extension( new SupportedExtension() ).extendedSort( "string" ).missing().last()
		);
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension( new SupportedExtension() ).extendedSort( "string" ).desc().missing().last()
		);
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Mandatory extension, unsupported
		assertThatThrownBy(
				() -> mainIndex.createScope().sort().extension( new UnSupportedExtension() )
		)
				.isInstanceOf( SearchException.class );

		// Conditional extensions with orElse - two, both supported
		query = simpleQuery( b -> b
				.extension()
				.ifSupported(
						new SupportedExtension(),
						c -> c.extendedSort( "string" ).missing().last()
				)
				.ifSupported(
						new SupportedExtension(),
						ignored -> fail( "This should not be called" )
				)
				.orElseFail()
		);
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
				.ifSupported(
						new SupportedExtension(),
						c -> c.extendedSort( "string" ).desc().missing().last()
				)
				.ifSupported(
						new SupportedExtension(),
						ignored -> fail( "This should not be called" )
				)
				.orElseFail()
		);
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Conditional extensions with orElse - two, second supported
		query = simpleQuery( b -> b
				.extension()
				.ifSupported(
						new UnSupportedExtension(),
						ignored -> fail( "This should not be called" )
				)
				.ifSupported(
						new SupportedExtension(),
						c -> c.extendedSort( "string" ).missing().last()
				)
				.orElse( ignored -> fail( "This should not be called" ) )
		);
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
				.ifSupported(
						new UnSupportedExtension(),
						ignored -> fail( "This should not be called" )
				)
				.ifSupported(
						new SupportedExtension(),
						c -> c.extendedSort( "string" ).desc().missing().last()
				)
				.orElse( ignored -> fail( "This should not be called" ) )
		);
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );

		// Conditional extensions with orElse - two, both unsupported
		query = simpleQuery( b -> b
				.extension()
				.ifSupported(
						new UnSupportedExtension(),
						ignored -> fail( "This should not be called" )
				)
				.ifSupported(
						new UnSupportedExtension(),
						ignored -> fail( "This should not be called" )
				)
				.orElse(
						c -> c.field( "string" ).missing().last()
				)
		);
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), FIRST_ID, SECOND_ID, THIRD_ID, EMPTY_ID );
		query = simpleQuery( b -> b
				.extension()
				.ifSupported(
						new UnSupportedExtension(),
						ignored -> fail( "This should not be called" )
				)
				.ifSupported(
						new UnSupportedExtension(),
						ignored -> fail( "This should not be called" )
				)
				.orElse(
						c -> c.field( "string" ).desc().missing().last()
				)
		);
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), THIRD_ID, SECOND_ID, FIRST_ID, EMPTY_ID );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void toAbsolutePath() {
		assertThat( mainIndex.createScope().sort().toAbsolutePath( "string" ) )
				.isEqualTo( "string" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void toAbsolutePath_withRoot() {
		assertThat( mainIndex.createScope().sort().withRoot( "flattened" ).toAbsolutePath( "string" ) )
				.isEqualTo( "flattened.string" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void toAbsolutePath_null() {
		assertThatThrownBy( () -> mainIndex.createScope().sort().toAbsolutePath( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'relativeFieldPath' must not be null" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void toAbsolutePath_withRoot_null() {
		assertThatThrownBy( () -> mainIndex.createScope().sort().withRoot( "flattened" ).toAbsolutePath( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'relativeFieldPath' must not be null" );
	}

	private void initData() {
		mainIndex.bulkIndexer()
				// Important: do not index the documents in the expected order after sorts
				.add( SECOND_ID, document -> {
					document.addValue( mainIndex.binding().string, "george" );
					document.addValue( mainIndex.binding().string_analyzed_forScore, "Hooray Hooray" );
					document.addValue( mainIndex.binding().string_analyzed_forScore_reversed, "Hooray Hooray" );
					document.addValue( mainIndex.binding().unsortable, "george" );
				} )
				.add( FIRST_ID, document -> {
					document.addValue( mainIndex.binding().string, "aaron" );
					document.addValue( mainIndex.binding().string_analyzed_forScore, "Hooray Hooray Hooray" );
					document.addValue( mainIndex.binding().string_analyzed_forScore_reversed, "Hooray" );
					document.addValue( mainIndex.binding().unsortable, "aaron" );
				} )
				.add( THIRD_ID, document -> {
					document.addValue( mainIndex.binding().string, "zach" );
					document.addValue( mainIndex.binding().string_analyzed_forScore, "Hooray" );
					document.addValue( mainIndex.binding().string_analyzed_forScore_reversed, "Hooray Hooray Hooray" );
					document.addValue( mainIndex.binding().unsortable, "zach" );
				} )
				.add( EMPTY_ID, document -> {} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> string_analyzed_forScore;
		final IndexFieldReference<String> string_analyzed_forScore_reversed;
		final IndexFieldReference<String> unsortable;
		final ObjectFieldBinding flattened;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
			string_analyzed_forScore = root.field(
					"string_analyzed_forScore",
					f -> f.asString()
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
			string_analyzed_forScore_reversed = root.field(
					"string_analyzed_forScore_reversed",
					f -> f.asString()
							.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
			unsortable = root.field( "unsortable", f -> f.asString().sortable( Sortable.NO ) )
					.toReference();

			flattened = new ObjectFieldBinding( root.objectField( "flattened", ObjectStructure.FLATTENED ) );
		}
	}

	private static class ObjectFieldBinding {
		final IndexObjectFieldReference self;
		final IndexFieldReference<String> string;

		ObjectFieldBinding(IndexSchemaObjectField objectField) {
			string = objectField.field( "string", f -> f.asString() ).toReference();
			self = objectField.toReference();
		}
	}

	private static class SupportedExtension implements SearchSortFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchSortFactory original) {
			assertThat( original ).isNotNull();
			return Optional.of( new MyExtendedFactory( original ) );
		}
	}

	private static class UnSupportedExtension implements SearchSortFactoryExtension<MyExtendedFactory> {
		@Override
		public Optional<MyExtendedFactory> extendOptional(SearchSortFactory original) {
			assertThat( original ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedFactory {
		private final SearchSortFactory delegate;

		MyExtendedFactory(SearchSortFactory delegate) {
			this.delegate = delegate;
		}

		public FieldSortOptionsStep<?, ? extends SearchPredicateFactory> extendedSort(String absoluteFieldPath) {
			return delegate.field( absoluteFieldPath );
		}
	}
}
