/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;

public final class AggregationRequestContext {

	private final ExtractionRequirements.Builder extractionRequirementsBuilder;
	private final Query luceneQuery;

	public AggregationRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder, Query luceneQuery) {
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
		this.luceneQuery = luceneQuery;
	}

	public Query getLuceneQuery() {
		return luceneQuery;
	}

	public <C extends Collector> void requireCollector(CollectorFactory<C> collectorFactory) {
		extractionRequirementsBuilder.requireCollectorForAllMatchingDocs( collectorFactory );
	}
}
