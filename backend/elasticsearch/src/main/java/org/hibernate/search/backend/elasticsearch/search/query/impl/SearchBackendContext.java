/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.HitExtractor;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.ObjectHitExtractor;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.ReferenceHitExtractor;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.util.EventContext;

public class SearchBackendContext {
	private final EventContext eventContext;

	private final ElasticsearchWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchWorkOrchestrator orchestrator;

	private final ObjectHitExtractor objectHitExtractor;
	private final ReferenceHitExtractor referenceHitExtractor;

	private final SearchProjectionBackendContext searchProjectionBackendContext;

	public SearchBackendContext(EventContext eventContext,
			ElasticsearchWorkFactory workFactory,
			Function<String, String> indexNameConverter,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkOrchestrator orchestrator) {
		this.eventContext = eventContext;
		this.workFactory = workFactory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;

		DocumentReferenceExtractorHelper documentReferenceExtractorHelper =
				new DocumentReferenceExtractorHelper( indexNameConverter, multiTenancyStrategy );

		this.objectHitExtractor = new ObjectHitExtractor( documentReferenceExtractorHelper );
		this.referenceHitExtractor = new ReferenceHitExtractor( documentReferenceExtractorHelper );

		this.searchProjectionBackendContext = new SearchProjectionBackendContext( documentReferenceExtractorHelper );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	ReferenceHitExtractor getReferenceHitExtractor() {
		return referenceHitExtractor;
	}

	ObjectHitExtractor getObjectHitExtractor() {
		return objectHitExtractor;
	}

	SearchProjectionBackendContext getSearchProjectionBackendContext() {
		return searchProjectionBackendContext;
	}

	<C, T> SearchQueryBuilderImpl<C, T> createSearchQueryBuilder(
			Set<URLEncodedString> indexNames,
			SessionContextImplementor sessionContext,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );
		return new SearchQueryBuilderImpl<>(
				workFactory, orchestrator, multiTenancyStrategy,
				indexNames, sessionContext, hitExtractor, hitAggregator
		);
	}

}
