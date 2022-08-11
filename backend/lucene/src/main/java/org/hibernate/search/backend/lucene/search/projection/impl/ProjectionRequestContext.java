/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ProjectionRequestContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ExtractionRequirements.Builder extractionRequirementsBuilder;
	private final String absoluteCurrentFieldPath;

	public ProjectionRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder) {
		this( extractionRequirementsBuilder, null );
	}

	private ProjectionRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder,
			String absoluteCurrentFieldPath) {
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
		this.absoluteCurrentFieldPath = absoluteCurrentFieldPath;
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

	public void checkValidField(String absoluteFieldPath) {
		if ( !FieldPaths.isStrictPrefix( absoluteCurrentFieldPath, absoluteFieldPath ) ) {
			throw log.invalidContextForProjectionOnField( absoluteFieldPath, absoluteCurrentFieldPath );
		}
	}

	public ProjectionRequestContext root() {
		return new ProjectionRequestContext( extractionRequirementsBuilder, null );
	}

	public ProjectionRequestContext forField(String absoluteFieldPath) {
		checkValidField( absoluteFieldPath );
		return new ProjectionRequestContext( extractionRequirementsBuilder, absoluteFieldPath );
	}

	public String absoluteCurrentFieldPath() {
		return absoluteCurrentFieldPath;
	}

}
