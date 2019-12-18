/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.LuceneCollectorFactory;

import org.apache.lucene.search.Collector;

public final class SearchProjectionRequestContext {

	private final ExtractionRequirements.Builder extractionRequirementsBuilder;

	public SearchProjectionRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder) {
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
	}

	public void requireAllStoredFields() {
		extractionRequirementsBuilder.requireAllStoredFields();
	}

	public void requireStoredField(String absoluteFieldPath) {
		extractionRequirementsBuilder.requireStoredField( absoluteFieldPath );
	}

	public void requireNestedDocumentExtraction(String nestedDocumentPath) {
		extractionRequirementsBuilder.requireNestedDocumentExtraction( nestedDocumentPath );
	}

	public void requireScore() {
		extractionRequirementsBuilder.requireScore();
	}

	public void requireTopDocsCollector() {
		extractionRequirementsBuilder.requireTopDocsCollector();
	}

	public <C extends Collector> void requireCollector(LuceneCollectorFactory<C> collectorFactory) {
		extractionRequirementsBuilder.requireCollector( collectorFactory );
	}
}
