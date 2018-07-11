/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneQueryWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.engine.search.query.spi.HitAggregator;

public class SearchBackendContext {
	private final EventContext eventContext;

	private final LuceneWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final LuceneQueryWorkOrchestrator orchestrator;

	public SearchBackendContext(EventContext eventContext,
			LuceneWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy,
			LuceneQueryWorkOrchestrator orchestrator) {
		this.eventContext = eventContext;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.workFactory = workFactory;
		this.orchestrator = orchestrator;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	<C, T> SearchQueryBuilderImpl<C, T> createSearchQueryBuilder(
			LuceneSearchTargetModel searchTargetModel,
			SessionContext sessionContext,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		Set<String> storedFields = new HashSet<>();
		hitExtractor.contributeFields( storedFields );

		return new SearchQueryBuilderImpl<>(
				workFactory,
				orchestrator,
				multiTenancyStrategy,
				searchTargetModel,
				sessionContext,
				new ReusableDocumentStoredFieldVisitor( storedFields ),
				hitExtractor,
				hitAggregator
		);
	}
}
