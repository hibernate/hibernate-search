/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.spi.FieldModelExtension;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateContainerContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl.ElasticsearchSearchPredicateContainerContextImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.spi.LoggerFactory;

public final class ElasticsearchExtension<N>
		implements SearchPredicateContainerContextExtension<N, ElasticsearchSearchPredicateContainerContext<N>>,
		FieldModelExtension<ElasticsearchIndexSchemaFieldContext> {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final ElasticsearchExtension INSTANCE = new ElasticsearchExtension();

	@SuppressWarnings("unchecked")
	public static <N> ElasticsearchExtension<N> get() {
		return INSTANCE;
	}

	private ElasticsearchExtension() {
		// Private constructor, use get() instead.
	}

	@Override
	public <C> ElasticsearchSearchPredicateContainerContext<N> extendOrFail(SearchPredicateContainerContext<N> original,
			SearchPredicateFactory<C> factory, SearchPredicateDslContext<N, C> dslContext) {
		if ( factory instanceof ElasticsearchSearchPredicateFactory ) {
			return extendUnsafe( original, (ElasticsearchSearchPredicateFactory) factory, dslContext );
		}
		else {
			throw log.elasticsearchExtensionOnUnknownType( factory );
		}
	}

	@Override
	public <C> Optional<ElasticsearchSearchPredicateContainerContext<N>> extendOptional(
			SearchPredicateContainerContext<N> original, SearchPredicateFactory<C> factory,
			SearchPredicateDslContext<N, C> dslContext) {
		if ( factory instanceof ElasticsearchSearchPredicateFactory ) {
			return Optional.of( extendUnsafe( original, (ElasticsearchSearchPredicateFactory) factory, dslContext ) );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public ElasticsearchIndexSchemaFieldContext extendOrFail(IndexSchemaFieldContext original) {
		if ( original instanceof ElasticsearchIndexSchemaFieldContext ) {
			return (ElasticsearchIndexSchemaFieldContext) original;
		}
		else {
			throw log.elasticsearchExtensionOnUnknownType( original );
		}
	}

	@SuppressWarnings("unchecked") // If the target is Elasticsearch, then we know C = ElasticsearchSearchPredicateCollector
	private <C> ElasticsearchSearchPredicateContainerContext<N> extendUnsafe(
			SearchPredicateContainerContext<N> original, ElasticsearchSearchPredicateFactory factory,
			SearchPredicateDslContext<N, C> dslContext) {
		return new ElasticsearchSearchPredicateContainerContextImpl<>(
				original, factory,
				(SearchPredicateDslContext<N, ElasticsearchSearchPredicateCollector>) dslContext
		);
	}
}
