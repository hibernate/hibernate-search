/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.query.spi.HitAggregator;

public class SearchBackendContext {
	// TODO use a dedicated object for the error context instead of the backend
	private final BackendImplementor<?> backend;

	private final ElasticsearchWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchWorkOrchestrator orchestrator;

	private final DocumentReferenceHitExtractor documentReferenceHitExtractor;
	private final ObjectHitExtractor objectHitExtractor;
	private final DocumentReferenceProjectionHitExtractor documentReferenceProjectionHitExtractor;

	public SearchBackendContext(BackendImplementor<?> backend,
			ElasticsearchWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkOrchestrator orchestrator) {
		this.backend = backend;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.workFactory = workFactory;
		this.orchestrator = orchestrator;

		this.documentReferenceHitExtractor = new DocumentReferenceHitExtractor( multiTenancyStrategy );
		this.objectHitExtractor = new ObjectHitExtractor( multiTenancyStrategy );
		this.documentReferenceProjectionHitExtractor = new DocumentReferenceProjectionHitExtractor( multiTenancyStrategy );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[backend=" + backend + "]";
	}

	DocumentReferenceHitExtractor getDocumentReferenceHitExtractor() {
		return documentReferenceHitExtractor;
	}

	ObjectHitExtractor getObjectHitExtractor() {
		return objectHitExtractor;
	}

	DocumentReferenceProjectionHitExtractor getDocumentReferenceProjectionHitExtractor() {
		return documentReferenceProjectionHitExtractor;
	}

	<C, T> SearchQueryBuilderImpl<C, T> createSearchQueryBuilder(
			Set<URLEncodedString> indexNames,
			SessionContext sessionContext,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		multiTenancyStrategy.checkTenantId( backend, sessionContext.getTenantIdentifier() );
		return new SearchQueryBuilderImpl<>(
				workFactory, orchestrator, multiTenancyStrategy,
				indexNames, sessionContext, hitExtractor, hitAggregator
		);
	}
}
