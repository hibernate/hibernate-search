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

import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.engine.search.query.spi.HitAggregator;

public class SearchBackendContext {
	private final EventContext eventContext;

	private final ElasticsearchWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchWorkOrchestrator orchestrator;

	private final DocumentReferenceHitExtractor documentReferenceHitExtractor;
	private final ObjectHitExtractor objectHitExtractor;
	private final DocumentReferenceProjectionHitExtractor documentReferenceProjectionHitExtractor;
	private final ScoreHitExtractor scoreHitExtractor;

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
		this.documentReferenceHitExtractor = new DocumentReferenceHitExtractor( documentReferenceExtractorHelper );
		this.objectHitExtractor = new ObjectHitExtractor( documentReferenceExtractorHelper );
		this.documentReferenceProjectionHitExtractor =
				new DocumentReferenceProjectionHitExtractor( documentReferenceExtractorHelper );
		this.scoreHitExtractor = new ScoreHitExtractor( documentReferenceExtractorHelper );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	public DocumentReferenceHitExtractor getDocumentReferenceHitExtractor() {
		return documentReferenceHitExtractor;
	}

	public ObjectHitExtractor getObjectHitExtractor() {
		return objectHitExtractor;
	}

	public DocumentReferenceProjectionHitExtractor getDocumentReferenceProjectionHitExtractor() {
		return documentReferenceProjectionHitExtractor;
	}

	public ScoreHitExtractor getScoreHitExtractor() {
		return scoreHitExtractor;
	}

	<C, T> SearchQueryBuilderImpl<C, T> createSearchQueryBuilder(
			Set<URLEncodedString> indexNames,
			SessionContext sessionContext,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );
		return new SearchQueryBuilderImpl<>(
				workFactory, orchestrator, multiTenancyStrategy,
				indexNames, sessionContext, hitExtractor, hitAggregator
		);
	}

}
