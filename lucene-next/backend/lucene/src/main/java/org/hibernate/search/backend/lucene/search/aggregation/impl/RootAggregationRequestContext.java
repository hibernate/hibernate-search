/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.query.spi.QueryParameters;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.Query;

public final class RootAggregationRequestContext implements AggregationRequestContext {

	private final LuceneSearchQueryIndexScope<?, ?> queryIndexScope;
	private final BackendSessionContext sessionContext;
	private final Set<String> routingKeys;
	private final ExtractionRequirements.Builder extractionRequirementsBuilder;
	private final QueryParameters parameters;

	public RootAggregationRequestContext(LuceneSearchQueryIndexScope<?, ?> queryIndexScope,
			BackendSessionContext sessionContext,
			Set<String> routingKeys, ExtractionRequirements.Builder extractionRequirementsBuilder,
			QueryParameters parameters) {
		this.queryIndexScope = queryIndexScope;
		this.sessionContext = sessionContext;
		this.routingKeys = routingKeys;
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
		this.parameters = parameters;
	}

	@Override
	public <C extends Collector, T, CM extends CollectorManager<C, T>> void requireCollector(
			CollectorFactory<C, T, CM> collectorFactory
	) {
		extractionRequirementsBuilder.requireCollectorForAllMatchingDocs( collectorFactory );
	}

	@Override
	public NamedValues queryParameters() {
		return parameters;
	}

	@Override
	public PredicateRequestContext toPredicateRequestContext(String absolutePath) {
		return PredicateRequestContext.withSession( queryIndexScope, sessionContext, routingKeys, parameters )
				.withNestedPath( absolutePath );
	}

	@Override
	public NestedDocsProvider createNestedDocsProvider(String nestedDocumentPath, Query nestedFilter) {
		return new NestedDocsProvider( nestedDocumentPath, nestedFilter );
	}
}
