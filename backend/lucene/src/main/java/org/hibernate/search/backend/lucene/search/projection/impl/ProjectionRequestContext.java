/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.backend.lucene.search.highlighter.impl.LuceneAbstractSearchHighlighter;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ProjectionRequestContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ExtractionRequirements.Builder extractionRequirementsBuilder;
	private final String absoluteCurrentNestedFieldPath;
	private final String absoluteCurrentFieldPath;
	private final LuceneAbstractSearchHighlighter globalHighlighter;
	private final Map<String, LuceneAbstractSearchHighlighter> namedHighlighters;

	public ProjectionRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder,
			LuceneAbstractSearchHighlighter globalHighlighter, Map<String, LuceneAbstractSearchHighlighter> namedHighlighters) {
		this( extractionRequirementsBuilder, globalHighlighter, namedHighlighters, null, null );
	}

	private ProjectionRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder,
			LuceneAbstractSearchHighlighter globalHighlighter, Map<String, LuceneAbstractSearchHighlighter> namedHighlighters,
			String absoluteCurrentFieldPath, String absoluteCurrentNestedFieldPath) {
		this.globalHighlighter = globalHighlighter;
		this.namedHighlighters = namedHighlighters;
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
		this.absoluteCurrentNestedFieldPath = absoluteCurrentNestedFieldPath;
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
		if ( !FieldPaths.isStrictPrefix( absoluteCurrentNestedFieldPath, absoluteFieldPath ) ) {
			throw log.invalidContextForProjectionOnField( absoluteFieldPath, absoluteCurrentNestedFieldPath );
		}
	}

	void checkNotNested(SearchQueryElementTypeKey<?> projectionKey, String hint) {
		if ( absoluteCurrentFieldPath() != null ) {
			throw log.cannotUseProjectionInNestedContext(
					projectionKey.toString(),
					hint,
					EventContexts.indexSchemaRoot()
			);
		}
	}

	public ProjectionRequestContext root() {
		return new ProjectionRequestContext( extractionRequirementsBuilder, globalHighlighter, namedHighlighters );
	}

	public ProjectionRequestContext forField(String absoluteFieldPath, boolean nestedObject) {
		checkValidField( absoluteFieldPath );
		return new ProjectionRequestContext(
				extractionRequirementsBuilder, globalHighlighter, namedHighlighters,
				absoluteFieldPath, nestedObject ? absoluteFieldPath : absoluteCurrentFieldPath
		);
	}

	public String absoluteCurrentNestedFieldPath() {
		return absoluteCurrentNestedFieldPath;
	}

	public String absoluteCurrentFieldPath() {
		return absoluteCurrentFieldPath;
	}

	public LuceneAbstractSearchHighlighter highlighter(String name) {
		if ( name == null ) {
			return globalHighlighter == null ? LuceneAbstractSearchHighlighter.defaultHighlighter() : globalHighlighter;
		}
		else {
			LuceneAbstractSearchHighlighter highlighter = namedHighlighters.get( name );
			if ( highlighter == null ) {
				throw log.cannotFindHighlighterWithName( name, namedHighlighters.keySet() );
			}
			return highlighter;
		}
	}

}
