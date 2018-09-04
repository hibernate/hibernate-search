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
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContextExtension;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateContainerContext;
import org.hibernate.search.backend.lucene.search.dsl.predicate.impl.LuceneSearchPredicateContainerContextImpl;
import org.hibernate.search.backend.lucene.search.dsl.sort.LuceneSearchSortContainerContext;
import org.hibernate.search.backend.lucene.search.dsl.sort.impl.LuceneSearchSortContainerContextImpl;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * An extension for the Lucene backend, giving access to Lucene-specific features.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called directly by users.
 * In short, users are only expected to get instances of this type from an API and pass it to another API.
 */
public final class LuceneExtension<N>
		implements SearchPredicateContainerContextExtension<N, LuceneSearchPredicateContainerContext<N>>,
		SearchSortContainerContextExtension<N, LuceneSearchSortContainerContext<N>>,
		IndexSchemaFieldContextExtension<LuceneIndexSchemaFieldContext> {

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> LuceneSearchPredicateContainerContext<N> extendOrFail(SearchPredicateContainerContext<N> original,
			SearchPredicateFactory<C, B> factory, SearchPredicateDslContext<N, ? super B> dslContext) {
		if ( factory instanceof LuceneSearchPredicateFactory ) {
			return extendUnsafe( original, (LuceneSearchPredicateFactory) factory, dslContext );
		}
		else {
			throw log.luceneExtensionOnUnknownType( factory );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<LuceneSearchPredicateContainerContext<N>> extendOptional(
			SearchPredicateContainerContext<N> original, SearchPredicateFactory<C, B> factory,
			SearchPredicateDslContext<N, ? super B> dslContext) {
		if ( factory instanceof LuceneSearchPredicateFactory ) {
			return Optional.of( extendUnsafe( original, (LuceneSearchPredicateFactory) factory, dslContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> LuceneSearchSortContainerContext<N> extendOrFail(SearchSortContainerContext<N> original,
			SearchSortFactory<C, B> factory, SearchSortDslContext<N, ? super B> dslContext) {
		if ( factory instanceof LuceneSearchSortFactory ) {
			return extendUnsafe( original, (LuceneSearchSortFactory) factory, dslContext );
		}
		else {
			throw log.luceneExtensionOnUnknownType( factory );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<LuceneSearchSortContainerContext<N>> extendOptional(
			SearchSortContainerContext<N> original, SearchSortFactory<C, B> factory,
			SearchSortDslContext<N, ? super B> dslContext) {
		if ( factory instanceof LuceneSearchSortFactory ) {
			return Optional.of( extendUnsafe( original, (LuceneSearchSortFactory) factory, dslContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LuceneIndexSchemaFieldContext extendOrFail(IndexSchemaFieldContext original) {
		if ( original instanceof LuceneIndexSchemaFieldContext ) {
			return (LuceneIndexSchemaFieldContext) original;
		}
		else {
			throw log.luceneExtensionOnUnknownType( original );
		}
	}

	@SuppressWarnings("unchecked") // If the target is Lucene, then we know B = LuceSearchPredicateBuilder
	private <B> LuceneSearchPredicateContainerContext<N> extendUnsafe(
			SearchPredicateContainerContext<N> original, LuceneSearchPredicateFactory factory,
			SearchPredicateDslContext<N, ? super B> dslContext) {
		return new LuceneSearchPredicateContainerContextImpl<>(
				original, factory,
				(SearchPredicateDslContext<N, ? super LuceneSearchPredicateBuilder>) dslContext
		);
	}

	@SuppressWarnings("unchecked") // If the target is Lucene, then we know B = LuceSearchSortBuilder
	private <B> LuceneSearchSortContainerContext<N> extendUnsafe(
			SearchSortContainerContext<N> original, LuceneSearchSortFactory factory,
			SearchSortDslContext<N, ? super B> dslContext) {
		return new LuceneSearchSortContainerContextImpl<>(
				original, factory,
				(SearchSortDslContext<N, ? super LuceneSearchSortBuilder>) dslContext
		);
	}
}
