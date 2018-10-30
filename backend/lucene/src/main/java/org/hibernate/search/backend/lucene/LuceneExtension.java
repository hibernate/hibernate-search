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
public final class LuceneExtension
		implements SearchPredicateContainerContextExtension<LuceneSearchPredicateContainerContext>,
		SearchSortContainerContextExtension<LuceneSearchSortContainerContext>,
		IndexSchemaFieldContextExtension<LuceneIndexSchemaFieldContext> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final LuceneExtension INSTANCE = new LuceneExtension();

	public static LuceneExtension get() {
		return INSTANCE;
	}

	private LuceneExtension() {
		// Private constructor, use get() instead.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <C, B> Optional<LuceneSearchPredicateContainerContext> extendOptional(
			SearchPredicateContainerContext original, SearchPredicateFactory<C, B> factory,
			SearchPredicateDslContext<? super B> dslContext) {
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
	public <C, B> Optional<LuceneSearchSortContainerContext> extendOptional(
			SearchSortContainerContext original, SearchSortFactory<C, B> factory,
			SearchSortDslContext<? super B> dslContext) {
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
	private <B> LuceneSearchPredicateContainerContext extendUnsafe(
			SearchPredicateContainerContext original, LuceneSearchPredicateFactory factory,
			SearchPredicateDslContext<? super B> dslContext) {
		return new LuceneSearchPredicateContainerContextImpl(
				original, factory,
				(SearchPredicateDslContext<? super LuceneSearchPredicateBuilder>) dslContext
		);
	}

	@SuppressWarnings("unchecked") // If the target is Lucene, then we know B = LuceSearchSortBuilder
	private <B> LuceneSearchSortContainerContext extendUnsafe(
			SearchSortContainerContext original, LuceneSearchSortFactory factory,
			SearchSortDslContext<? super B> dslContext) {
		return new LuceneSearchSortContainerContextImpl(
				original, factory,
				(SearchSortDslContext<? super LuceneSearchSortBuilder>) dslContext
		);
	}
}
