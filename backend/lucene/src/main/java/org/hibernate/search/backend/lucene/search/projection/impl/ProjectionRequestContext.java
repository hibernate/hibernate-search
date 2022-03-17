/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;

public final class ProjectionRequestContext {

	private final ExtractionRequirements.Builder extractionRequirementsBuilder;

	public ProjectionRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder) {
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
	}

	public void requireAllStoredFields() {
		extractionRequirementsBuilder.requireAllStoredFields();
	}

	public void requireStoredField(String absoluteFieldPath, String nestedDocumentPath) {
		extractionRequirementsBuilder.requireStoredField( absoluteFieldPath, nestedDocumentPath );
	}

	public void requireScore() {
		extractionRequirementsBuilder.requireScore();
	}

}
