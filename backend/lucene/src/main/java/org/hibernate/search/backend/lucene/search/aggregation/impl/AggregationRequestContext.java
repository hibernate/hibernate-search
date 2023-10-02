/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.query.spi.QueryParameters;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public final class AggregationRequestContext {

	private final ExtractionRequirements.Builder extractionRequirementsBuilder;
	private final QueryParameters parameters;

	public AggregationRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder,
			QueryParameters parameters) {
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
		this.parameters = parameters;
	}

	public <C extends Collector, T, CM extends CollectorManager<C, T>> void requireCollector(
			CollectorFactory<C, T, CM> collectorFactory) {
		extractionRequirementsBuilder.requireCollectorForAllMatchingDocs( collectorFactory );
	}

	public NamedValues queryParameters() {
		return parameters;
	}
}
