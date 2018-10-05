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
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.ReferenceHitExtractor;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.HitExtractor;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.ObjectHitExtractor;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DocumentReferenceSearchProjectionBuilderImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ObjectSearchProjectionBuilderImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ReferenceSearchProjectionBuilderImpl;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ScoreSearchProjectionBuilderImpl;
import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.util.EventContext;

public class SearchBackendContext {
	private final EventContext eventContext;

	private final ElasticsearchWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchWorkOrchestrator orchestrator;

	private final ObjectHitExtractor objectHitExtractor;
	private final ReferenceHitExtractor referenceHitExtractor;

	private final DocumentReferenceSearchProjectionBuilderImpl documentReferenceProjectionBuilder;
	private final ObjectSearchProjectionBuilderImpl objectProjectionBuilder;
	private final ReferenceSearchProjectionBuilderImpl referenceProjectionBuilder;
	private final ScoreSearchProjectionBuilderImpl scoreProjectionBuilder;

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

		this.documentReferenceProjectionBuilder = new DocumentReferenceSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.objectProjectionBuilder = new ObjectSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.referenceProjectionBuilder = new ReferenceSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.scoreProjectionBuilder = new ScoreSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	public ReferenceHitExtractor getReferenceHitExtractor() {
		return referenceHitExtractor;
	}

	public ObjectHitExtractor getObjectHitExtractor() {
		return objectHitExtractor;
	}

	public DocumentReferenceSearchProjectionBuilderImpl getDocumentReferenceProjectionBuilder() {
		return documentReferenceProjectionBuilder;
	}

	public ObjectSearchProjectionBuilderImpl getObjectProjectionBuilder() {
		return objectProjectionBuilder;
	}

	public ReferenceSearchProjectionBuilderImpl getReferenceProjectionBuilder() {
		return referenceProjectionBuilder;
	}

	public ScoreSearchProjectionBuilderImpl getScoreProjectionBuilder() {
		return scoreProjectionBuilder;
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
