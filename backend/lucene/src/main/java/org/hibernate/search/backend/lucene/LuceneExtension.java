/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.FieldModelExtension;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateContainerContext;
import org.hibernate.search.backend.lucene.search.dsl.predicate.impl.LuceneSearchPredicateContainerContextImpl;
import org.hibernate.search.backend.lucene.search.dsl.sort.LuceneSearchSortContainerContext;
import org.hibernate.search.backend.lucene.search.dsl.sort.impl.LuceneSearchSortContainerContextImpl;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateCollector;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

public final class LuceneExtension<N>
		implements SearchPredicateContainerContextExtension<N, LuceneSearchPredicateContainerContext<N>>,
		SearchSortContainerContextExtension<N, LuceneSearchSortContainerContext<N>>,
		FieldModelExtension<LuceneIndexSchemaFieldContext> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@SuppressWarnings("rawtypes")
	private static final LuceneExtension INSTANCE = new LuceneExtension();

	@SuppressWarnings("unchecked")
	public static <N> LuceneExtension<N> get() {
		return INSTANCE;
	}

	private LuceneExtension() {
		// Private constructor, use get() instead.
	}

	@Override
	public <C> LuceneSearchPredicateContainerContext<N> extendOrFail(SearchPredicateContainerContext<N> original,
			SearchPredicateFactory<C> factory, SearchPredicateDslContext<N, ? extends C> dslContext) {
		if ( factory instanceof LuceneSearchPredicateFactory ) {
			return extendUnsafe( original, (LuceneSearchPredicateFactory) factory, dslContext );
		}
		else {
			throw log.luceneExtensionOnUnknownType( factory );
		}
	}

	@Override
	public <C> Optional<LuceneSearchPredicateContainerContext<N>> extendOptional(
			SearchPredicateContainerContext<N> original, SearchPredicateFactory<C> factory,
			SearchPredicateDslContext<N, ? extends C> dslContext) {
		if ( factory instanceof LuceneSearchPredicateFactory ) {
			return Optional.of( extendUnsafe( original, (LuceneSearchPredicateFactory) factory, dslContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public <C> LuceneSearchSortContainerContext<N> extendOrFail(SearchSortContainerContext<N> original,
			SearchSortFactory<C> factory, SearchSortDslContext<N, ? extends C> dslContext) {
		if ( factory instanceof LuceneSearchSortFactory ) {
			return extendUnsafe( original, (LuceneSearchSortFactory) factory, dslContext );
		}
		else {
			throw log.luceneExtensionOnUnknownType( factory );
		}
	}

	@Override
	public <C> Optional<LuceneSearchSortContainerContext<N>> extendOptional(
			SearchSortContainerContext<N> original, SearchSortFactory<C> factory,
			SearchSortDslContext<N, ? extends C> dslContext) {
		if ( factory instanceof LuceneSearchSortFactory ) {
			return Optional.of( extendUnsafe( original, (LuceneSearchSortFactory) factory, dslContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public LuceneIndexSchemaFieldContext extendOrFail(IndexSchemaFieldContext original) {
		if ( original instanceof LuceneIndexSchemaFieldContext ) {
			return (LuceneIndexSchemaFieldContext) original;
		}
		else {
			throw log.luceneExtensionOnUnknownType( original );
		}
	}

	@SuppressWarnings("unchecked") // If the target is Lucene, then we know C = LuceneSearchPredicateCollector
	private <C> LuceneSearchPredicateContainerContext<N> extendUnsafe(
			SearchPredicateContainerContext<N> original, LuceneSearchPredicateFactory factory,
			SearchPredicateDslContext<N, C> dslContext) {
		return new LuceneSearchPredicateContainerContextImpl<>(
				original, factory,
				(SearchPredicateDslContext<N, ? extends LuceneSearchPredicateCollector>) dslContext
		);
	}

	@SuppressWarnings("unchecked") // If the target is Lucene, then we know C = LuceneSearchSortCollector
	private <C> LuceneSearchSortContainerContext<N> extendUnsafe(
			SearchSortContainerContext<N> original, LuceneSearchSortFactory factory,
			SearchSortDslContext<N, C> dslContext) {
		return new LuceneSearchSortContainerContextImpl<>(
				original, factory,
				(SearchSortDslContext<N, ? extends LuceneSearchSortCollector>) dslContext
		);
	}
}
