/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public final class AggregationRequestContext {

	private final ExtractionRequirements.Builder extractionRequirementsBuilder;

	public AggregationRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder) {
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
	}

	public <C extends Collector, T, CM extends CollectorManager<C, T>> void requireCollector(
			CollectorFactory<C, T, CM> collectorFactory) {
		extractionRequirementsBuilder.requireCollectorForAllMatchingDocs( collectorFactory );
	}
}
