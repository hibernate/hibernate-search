/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String ANOTHER_INDEX_NAME = "AnotherIndexName";

	private static final String DOCUMENT_1 = "doc1";
	private static final String DOCUMENT_2 = "doc2";
	private static final String EMPTY = "empty";

	private static final String STRING_1 = "Irving";
	private static final String STRING_2 = "Auster";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

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

	@Test
	public void match_fluid() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.match().onField( "string" ).matching( STRING_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_search_predicate() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate = scope.predicate().match().onField( "string" ).matching( STRING_1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( predicate )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_lambda() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.match().onField( "string" ).matching( STRING_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void reuseRootPredicateInstance_onScopeTargetingSameIndexes() {
		StubMappingScope scope = indexManager.createScope();
		SearchPredicate predicate = scope
				.predicate().match().onField( "string" ).matching( STRING_1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( predicate )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// reuse the same predicate instance on the same scope
		query = scope.query()
				.predicate( predicate )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// reuse the same predicate instance on a different scope,
		// targeting the same index
		query = indexManager.createScope().query()
				.predicate( predicate )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		predicate = indexManager.createScope( anotherIndexManager )
				.predicate().match().onField( "string" ).matching( STRING_1 ).toPredicate();

		// reuse the same predicate instance on a different scope,
		// targeting same indexes
		query = anotherIndexManager.createScope( indexManager ).query()
				.predicate( predicate )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void reuseRootPredicateInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = indexManager.createScope();
		SearchPredicate predicate = scope
				.predicate().match().onField( "string" ).matching( STRING_1 ).toPredicate();

		// reuse the same predicate instance on a different scope,
		// targeting a different index
		SubTest.expectException( () ->
				anotherIndexManager.createScope().query()
						.predicate( predicate )
						.toQuery() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( ANOTHER_INDEX_NAME );

		// reuse the same predicate instance on a different scope,
		// targeting different indexes
		SubTest.expectException( () ->
				indexManager.createScope( anotherIndexManager ).query()
						.predicate( predicate )
						.toQuery() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( ANOTHER_INDEX_NAME );
	}

	@Test
	public void reuseNonRootPredicateInstance_onScopeTargetingSameIndexes() {
		StubMappingScope scope = indexManager.createScope();
		final SearchPredicate predicate = scope
				.predicate().match().onField( "string" ).matching( STRING_1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.bool().must( predicate ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// reuse the same predicate instance on the same scope
		query = scope.query()
				.predicate( f -> f.bool().must( predicate ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// reuse the same predicate instance on a different scope,
		// targeting the same index
		query = indexManager.createScope().query()
				.predicate( f -> f.bool().must( predicate ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		final SearchPredicate multiIndexScopedPredicate = indexManager.createScope( anotherIndexManager )
				.predicate().match().onField( "string" ).matching( STRING_1 ).toPredicate();

		// reuse the same predicate instance on a different scope,
		// targeting same indexes
		query = anotherIndexManager.createScope( indexManager ).query()
				.predicate( f -> f.bool().must( multiIndexScopedPredicate ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = anotherIndexManager.createScope( indexManager ).query()
				.predicate( f -> f.bool()
						.should( multiIndexScopedPredicate )
						.should( f.match().onField( "string" ).matching( STRING_2 ) )
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void reuseNonRootPredicateInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = indexManager.createScope();
		SearchPredicate predicate = scope
				.predicate().match().onField( "string" ).matching( STRING_1 ).toPredicate();

		// reuse the same predicate instance on a different scope,
		// targeting a different index
		SubTest.expectException( () ->
				anotherIndexManager.createScope().query()
						.predicate( f -> f.bool().must( predicate ) )
						.toQuery() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( ANOTHER_INDEX_NAME );

		// reuse the same predicate instance on a different scope,
		// targeting different indexes
		SubTest.expectException( () ->
				indexManager.createScope( anotherIndexManager ).query()
						.predicate( f -> f.bool().must( predicate ) )
						.toQuery() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( ANOTHER_INDEX_NAME );
	}

	@Test
	public void extension() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query;

		// Mandatory extension, supported
		query = scope.query()
				.predicate( f -> f.extension( new SupportedExtension() )
						.extendedPredicate( "string", STRING_1 )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Mandatory extension, unsupported
		SubTest.expectException(
				() -> scope.predicate().extension( new UnSupportedExtension() )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class );

		// Conditional extensions with orElse - two, both supported
		query = scope.query()
				.predicate( f -> f.extension()
						.ifSupported(
								new SupportedExtension(),
								extended -> extended.extendedPredicate( "string", STRING_1 )
						)
						.ifSupported(
								new SupportedExtension(),
								shouldNotBeCalled()
						)
						.orElseFail()
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Conditional extensions with orElse - two, second supported
		query = scope.query()
				.predicate( f -> f.extension()
						.ifSupported(
								new UnSupportedExtension(),
								shouldNotBeCalled()
						)
						.ifSupported(
								new SupportedExtension(),
								extended -> extended.extendedPredicate( "string", STRING_1 )
						)
						.orElse(
								shouldNotBeCalled()
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Conditional extensions with orElse - two, both unsupported
		query = scope.query()
				.predicate( f -> f.extension()
						.ifSupported(
								new UnSupportedExtension(),
								shouldNotBeCalled()
						)
						.ifSupported(
								new UnSupportedExtension(),
								shouldNotBeCalled()
						)
						.orElse(
								c -> c.match().onField( "string" ).matching( STRING_1 )
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.string, STRING_1 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.string, STRING_2 );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY );
	}

	private static <T, R> Function<T, R> shouldNotBeCalled() {
		return ignored -> {
			throw new IllegalStateException( "This should not be called" );
		};
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
		}
	}

	private static class SupportedExtension implements SearchPredicateFactoryExtension<MyExtendedFactory> {
		@Override
		public <C, B> Optional<MyExtendedFactory> extendOptional(SearchPredicateFactory original,
				SearchPredicateBuilderFactory<C, B> factory) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			return Optional.of( new MyExtendedFactory( original ) );
		}
	}

	private static class UnSupportedExtension implements SearchPredicateFactoryExtension<MyExtendedFactory> {
		@Override
		public <C, B> Optional<MyExtendedFactory> extendOptional(SearchPredicateFactory original,
				SearchPredicateBuilderFactory<C, B> factory) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedFactory {
		private final SearchPredicateFactory delegate;

		MyExtendedFactory(SearchPredicateFactory delegate) {
			this.delegate = delegate;
		}

		public PredicateFinalStep extendedPredicate(String fieldName, String value) {
			return delegate.match().onField( fieldName ).matching( value );
		}
	}
}
