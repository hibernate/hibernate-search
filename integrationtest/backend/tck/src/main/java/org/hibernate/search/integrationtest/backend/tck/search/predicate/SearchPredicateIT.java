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
import java.util.function.Consumer;

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
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateContainerContextImpl;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

import org.junit.Assert;
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
				.predicate( root -> root.match().onField( "string" ).matching( STRING_1 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_search_predicate() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchPredicate predicate = searchTarget.predicate().match().onField( "string" ).matching( STRING_1 ).end();

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
				.predicate( root -> root.match().onField( "string" ).matching( STRING_1 ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_lambda_caching_root() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		AtomicReference<SearchPredicate> cache = new AtomicReference<>();

		Consumer<? super SearchPredicateContainerContext> cachingContributor = c -> {
			if ( cache.get() == null ) {
				SearchPredicate result = c.match().onField( "string" ).matching( STRING_1 ).toPredicate();
				cache.set( result );
			}
			else {
				c.predicate( cache.get() );
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

		Consumer<? super SearchPredicateContainerContext> cachingContributor = c -> {
			if ( cache.get() == null ) {
				SearchPredicate result = c.match().onField( "string" ).matching( STRING_1 ).toPredicate();
				cache.set( result );
			}
			else {
				c.predicate( cache.get() );
			}
		};

		Assertions.assertThat( cache ).hasValue( null );

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool().must( cachingContributor ) )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		Assertions.assertThat( cache ).doesNotHaveValue( null );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.should( cachingContributor )
						.should( c -> c.match().onField( "string" ).matching( STRING_2 ) )
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
				.predicate( root -> root.extension( new SupportedExtension() )
						.match().onField( "string" ).matching( STRING_1 )
				)
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Conditional extensions with orElse - two, both supported
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.extension()
						// FIXME find some way to forbid using the context passed to the consumers twice... ?
						.ifSupported(
								new SupportedExtension(),
								c -> c.match().onField( "string" ).matching( STRING_1 ).end()
						)
						.ifSupported(
								new SupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
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
								ignored -> Assert.fail( "This should not be called" )
						)
						.ifSupported(
								new SupportedExtension(),
								c -> c.match().onField( "string" ).matching( STRING_1 ).end()
						)
						.orElse(
								ignored -> Assert.fail( "This should not be called" )
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
								ignored -> Assert.fail( "This should not be called" )
						)
						.ifSupported(
								new UnSupportedExtension(),
								ignored -> Assert.fail( "This should not be called" )
						)
						.orElse(
								c -> c.match().onField( "string" ).matching( STRING_1 ).end()
						)
				)
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Conditional extensions without orElse - one, unsupported
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.must( c -> c.extension()
								.ifSupported(
										new UnSupportedExtension(),
										ignored -> Assert.fail( "This should not be called" )
								)
						)
						.must(
								c -> c.match().onField( "string" ).matching( STRING_1 ).end()
						)
						.end()
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
				.predicate( root -> root.matchAll() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
		}
	}

	private static class SupportedExtension implements SearchPredicateContainerContextExtension<MyExtendedContext> {
		@Override
		public <C, B> Optional<MyExtendedContext> extendOptional(SearchPredicateContainerContext original,
				SearchPredicateFactory<C, B> factory, SearchPredicateDslContext<? super B> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			return Optional.of( new MyExtendedContext( original ) );
		}
	}

	private static class UnSupportedExtension implements SearchPredicateContainerContextExtension<MyExtendedContext> {
		@Override
		public <C, B> Optional<MyExtendedContext> extendOptional(SearchPredicateContainerContext original,
				SearchPredicateFactory<C, B> factory, SearchPredicateDslContext<? super B> dslContext) {
			Assertions.assertThat( original ).isNotNull();
			Assertions.assertThat( factory ).isNotNull();
			Assertions.assertThat( dslContext ).isNotNull();
			return Optional.empty();
		}
	}

	private static class MyExtendedContext extends DelegatingSearchPredicateContainerContextImpl {
		MyExtendedContext(SearchPredicateContainerContext delegate) {
			super( delegate );
		}
	}
}
