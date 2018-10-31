/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "doc1";
	private static final String DOCUMENT_2 = "doc2";
	private static final String EMPTY = "empty";

	private static final String STRING_1 = "Irving";
	private static final String STRING_2 = "Auster";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexAccessors indexAccessors;
	private MappedIndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

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

	@Test
	public void match_fluid() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onField( "string" ).matching( STRING_1 ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_search_predicate() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchPredicate predicate = searchTarget.predicate().match().onField( "string" ).matching( STRING_1 ).toPredicate();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( predicate )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_lambda() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onField( "string" ).matching( STRING_1 ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_lambda_caching_root() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		AtomicReference<SearchPredicate> cache = new AtomicReference<>();

		Function<? super SearchPredicateFactoryContext, SearchPredicate> cachingContributor = c -> {
			if ( cache.get() == null ) {
				SearchPredicate result = c.match().onField( "string" ).matching( STRING_1 ).toPredicate();
				cache.set( result );
				return result;
			}
			else {
				return cache.get();
			}
		};

		Assertions.assertThat( cache ).hasValue( null );

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( cachingContributor )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		Assertions.assertThat( cache ).doesNotHaveValue( null );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( cachingContributor )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_lambda_caching_nonRoot() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		AtomicReference<SearchPredicate> cache = new AtomicReference<>();

		Function<? super SearchPredicateFactoryContext, SearchPredicate> cachingContributor = c -> {
			if ( cache.get() == null ) {
				SearchPredicate result = c.match().onField( "string" ).matching( STRING_1 ).toPredicate();
				cache.set( result );
				return result;
			}
			else {
				return cache.get();
			}
		};

		Assertions.assertThat( cache ).hasValue( null );

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool().must( cachingContributor ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		Assertions.assertThat( cache ).doesNotHaveValue( null );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.should( cachingContributor )
						.should( f.match().onField( "string" ).matching( STRING_2 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void extension() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query;

		// Mandatory extension
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.extension( new SupportedExtension() )
						.extendedPredicate( "string", STRING_1 )
				)
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Conditional extensions with orElse - two, both supported
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.extension()
						// FIXME find some way to forbid using the context passed to the consumers twice... ?
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
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Conditional extensions with orElse - two, second supported
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.extension()
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
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Conditional extensions with orElse - two, both unsupported
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.extension()
						.ifSupported(
								new UnSupportedExtension(),
								shouldNotBeCalled()
						)
						.ifSupported(
								new UnSupportedExtension(),
								shouldNotBeCalled()
						)
						.orElse(
								c -> c.match().onField( "string" ).matching( STRING_1 ).toPredicate()
						)
				)
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexAccessors.string.write( document, STRING_1 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexAccessors.string.write( document, STRING_2 );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY );
	}

	private static <T, R> Function<T, R> shouldNotBeCalled() {
		return ignored -> {
			throw new IllegalStateException( "This should not be called" );
		};
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
		}
	}

	private static class SupportedExtension implements SearchPredicateFactoryContextExtension<MyExtendedContext> {
		@Override
		public <C, B> Optional<MyExtendedContext> extendOptional(SearchPredicateFactoryContext original,
				SearchPredicateBuilderFactory<C, B> factory) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			return Optional.of( new MyExtendedContext( original ) );
		}
	}

	private static class UnSupportedExtension implements SearchPredicateFactoryContextExtension<MyExtendedContext> {
		@Override
		public <C, B> Optional<MyExtendedContext> extendOptional(SearchPredicateFactoryContext original,
				SearchPredicateBuilderFactory<C, B> factory) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedContext {
		private final SearchPredicateFactoryContext delegate;

		MyExtendedContext(SearchPredicateFactoryContext delegate) {
			this.delegate = delegate;
		}

		public SearchPredicate extendedPredicate(String fieldName, String value) {
			return delegate.match().onField( fieldName ).matching( value ).toPredicate();
		}
	}
}
