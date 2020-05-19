/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchPredicateIT {

	private static final String DOCUMENT_1 = "doc1";
	private static final String DOCUMENT_2 = "doc2";
	private static final String EMPTY = "empty";

	private static final String STRING_1 = "Irving";
	private static final String STRING_2 = "Auster";

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

	@Test
	public void where_searchPredicate() {
		StubMappingScope scope = mainIndex.createScope();

		SearchPredicate predicate = scope.predicate().match().field( "string" ).matching( STRING_1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where( predicate )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void where_lambda() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( "string" ).matching( STRING_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void predicate_searchPredicate() {
		StubMappingScope scope = mainIndex.createScope();

		SearchPredicate predicate = scope.predicate().match().field( "string" ).matching( STRING_1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( predicate )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void predicate_lambda() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.match().field( "string" ).matching( STRING_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void reuseRootPredicateInstance_onScopeTargetingSameIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		SearchPredicate predicate = scope
				.predicate().match().field( "string" ).matching( STRING_1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where( predicate )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// reuse the same predicate instance on the same scope
		query = scope.query()
				.where( predicate )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// reuse the same predicate instance on a different scope,
		// targeting the same index
		query = mainIndex.createScope().query()
				.where( predicate )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		predicate = mainIndex.createScope( otherIndex )
				.predicate().match().field( "string" ).matching( STRING_1 ).toPredicate();

		// reuse the same predicate instance on a different scope,
		// targeting same indexes
		query = otherIndex.createScope( mainIndex ).query()
				.where( predicate )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void reuseRootPredicateInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		SearchPredicate predicate = scope
				.predicate().match().field( "string" ).matching( STRING_1 ).toPredicate();

		// reuse the same predicate instance on a different scope,
		// targeting a different index
		Assertions.assertThatThrownBy( () ->
				otherIndex.createScope().query()
						.where( predicate )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( mainIndex.name() )
				.hasMessageContaining( otherIndex.name() );

		// reuse the same predicate instance on a different scope,
		// targeting different indexes
		Assertions.assertThatThrownBy( () ->
				mainIndex.createScope( otherIndex ).query()
						.where( predicate )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( mainIndex.name() )
				.hasMessageContaining( otherIndex.name() );
	}

	@Test
	public void reuseNonRootPredicateInstance_onScopeTargetingSameIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		final SearchPredicate predicate = scope
				.predicate().match().field( "string" ).matching( STRING_1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool().must( predicate ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// reuse the same predicate instance on the same scope
		query = scope.query()
				.where( f -> f.bool().must( predicate ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// reuse the same predicate instance on a different scope,
		// targeting the same index
		query = mainIndex.createScope().query()
				.where( f -> f.bool().must( predicate ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		final SearchPredicate multiIndexScopedPredicate = mainIndex.createScope( otherIndex )
				.predicate().match().field( "string" ).matching( STRING_1 ).toPredicate();

		// reuse the same predicate instance on a different scope,
		// targeting same indexes
		query = otherIndex.createScope( mainIndex ).query()
				.where( f -> f.bool().must( multiIndexScopedPredicate ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		query = otherIndex.createScope( mainIndex ).query()
				.where( f -> f.bool()
						.should( multiIndexScopedPredicate )
						.should( f.match().field( "string" ).matching( STRING_2 ) )
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void reuseNonRootPredicateInstance_onScopeTargetingDifferentIndexes() {
		StubMappingScope scope = mainIndex.createScope();
		SearchPredicate predicate = scope
				.predicate().match().field( "string" ).matching( STRING_1 ).toPredicate();

		// reuse the same predicate instance on a different scope,
		// targeting a different index
		Assertions.assertThatThrownBy( () ->
				otherIndex.createScope().query()
						.where( f -> f.bool().must( predicate ) )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( mainIndex.name() )
				.hasMessageContaining( otherIndex.name() );

		// reuse the same predicate instance on a different scope,
		// targeting different indexes
		Assertions.assertThatThrownBy( () ->
				mainIndex.createScope( otherIndex ).query()
						.where( f -> f.bool().must( predicate ) )
						.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( mainIndex.name() )
				.hasMessageContaining( otherIndex.name() );
	}

	@Test
	public void extension() {
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query;

		// Mandatory extension, supported
		query = scope.query()
				.where( f -> f.extension( new SupportedExtension() )
						.extendedPredicate( "string", STRING_1 )
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// Mandatory extension, unsupported
		Assertions.assertThatThrownBy(
				() -> scope.predicate().extension( new UnSupportedExtension() )
		)
				.isInstanceOf( SearchException.class );

		// Conditional extensions with orElse - two, both supported
		query = scope.query()
				.where( f -> f.extension()
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
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// Conditional extensions with orElse - two, second supported
		query = scope.query()
				.where( f -> f.extension()
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
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// Conditional extensions with orElse - two, both unsupported
		query = scope.query()
				.where( f -> f.extension()
						.ifSupported(
								new UnSupportedExtension(),
								shouldNotBeCalled()
						)
						.ifSupported(
								new UnSupportedExtension(),
								shouldNotBeCalled()
						)
						.orElse(
								c -> c.match().field( "string" ).matching( STRING_1 )
						)
				)
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	private void initData() {
		mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					document.addValue( mainIndex.binding().string, STRING_1 );
				} )
				.add( DOCUMENT_2, document -> {
					document.addValue( mainIndex.binding().string, STRING_2 );
				} )
				.add( EMPTY, document -> { } )
				.join();

		// Check that all documents are searchable
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, EMPTY );
	}

	private static <T, R> Function<T, R> shouldNotBeCalled() {
		return ignored -> {
			throw new IllegalStateException( "This should not be called" );
		};
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
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
			return delegate.match().field( fieldName ).matching( value );
		}
	}
}
