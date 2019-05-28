/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneReadWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.util.common.reporting.EventContext;

public class SearchBackendContext {
	private final EventContext eventContext;

	private final LuceneWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final LuceneReadWorkOrchestrator orchestrator;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	public SearchBackendContext(EventContext eventContext,
			LuceneWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy,
			LuceneReadWorkOrchestrator orchestrator, LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.eventContext = eventContext;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.workFactory = workFactory;
		this.orchestrator = orchestrator;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	public LuceneSearchContext createSearchContext(MappingContextImplementor mappingContext,
			LuceneScopeModel scopeModel) {
		return new LuceneSearchContext(
				mappingContext, analysisDefinitionRegistry, multiTenancyStrategy, scopeModel
		);
	}

	<H> LuceneSearchQueryBuilder<H> createSearchQueryBuilder(
			LuceneSearchContext searchContext,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<?, H> rootProjection) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		LuceneDocumentStoredFieldVisitorBuilder storedFieldFilterBuilder = new LuceneDocumentStoredFieldVisitorBuilder();
		rootProjection.contributeFields( storedFieldFilterBuilder );

		return new LuceneSearchQueryBuilder<>(
				workFactory,
				orchestrator,
				searchContext,
				sessionContext,
				storedFieldFilterBuilder.build(),
				loadingContextBuilder,
				rootProjection
		);
	}
}
