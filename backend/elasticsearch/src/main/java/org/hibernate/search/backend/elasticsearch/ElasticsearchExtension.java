/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.spi.FieldModelContext;
import org.hibernate.search.engine.backend.document.model.spi.FieldModelExtension;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchFieldModelContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.ElasticsearchSearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.util.spi.LoggerFactory;

public final class ElasticsearchExtension<N>
		implements SearchPredicateContainerContextExtension<N, ElasticsearchSearchPredicateContainerContext<N>>,
		FieldModelExtension<ElasticsearchFieldModelContext> {

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
	public ElasticsearchSearchPredicateContainerContext<N> extendOrFail(SearchPredicateContainerContext<N> original) {
		if ( original instanceof ElasticsearchSearchPredicateContainerContext ) {
			return (ElasticsearchSearchPredicateContainerContext<N>) original;
		}
		else {
			throw log.elasticsearchExtensionOnUnknownContext( original );
		}
	}

	@Override
	public Optional<ElasticsearchSearchPredicateContainerContext<N>> extendOptional(SearchPredicateContainerContext<N> original) {
		if ( original instanceof ElasticsearchSearchPredicateContainerContext ) {
			return Optional.of( (ElasticsearchSearchPredicateContainerContext<N>) original );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public ElasticsearchFieldModelContext extendOrFail(FieldModelContext original) {
		if ( original instanceof ElasticsearchFieldModelContext ) {
			return (ElasticsearchFieldModelContext) original;
		}
		else {
			throw log.elasticsearchExtensionOnUnknownContext( original );
		}
	}
}
