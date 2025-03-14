/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.backend.lucene.search.highlighter.impl.LuceneAbstractSearchHighlighter;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.query.spi.QueryParameters;

public final class ProjectionRequestContext {

	private final ExtractionRequirements.Builder extractionRequirementsBuilder;
	private final String absoluteCurrentNestedFieldPath;
	private final String absoluteCurrentFieldPath;
	private final LuceneAbstractSearchHighlighter globalHighlighter;
	private final Map<String, LuceneAbstractSearchHighlighter> namedHighlighters;
	private final QueryParameters parameters;

	public ProjectionRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder,
			LuceneAbstractSearchHighlighter globalHighlighter, Map<String, LuceneAbstractSearchHighlighter> namedHighlighters,
			QueryParameters parameters) {
		this( extractionRequirementsBuilder, globalHighlighter, namedHighlighters, parameters, null, null );
	}

	private ProjectionRequestContext(ExtractionRequirements.Builder extractionRequirementsBuilder,
			LuceneAbstractSearchHighlighter globalHighlighter, Map<String, LuceneAbstractSearchHighlighter> namedHighlighters,
			QueryParameters parameters,
			String absoluteCurrentFieldPath, String absoluteCurrentNestedFieldPath) {
		this.extractionRequirementsBuilder = extractionRequirementsBuilder;
		this.globalHighlighter = globalHighlighter;
		this.namedHighlighters = namedHighlighters;
		this.parameters = parameters;
		this.absoluteCurrentNestedFieldPath = absoluteCurrentNestedFieldPath;
		this.absoluteCurrentFieldPath = absoluteCurrentFieldPath;
	}

	public void requireAllStoredFields() {
		extractionRequirementsBuilder.requireAllStoredFields();
	}

	public void requireNestedObjects(Collection<String> paths) {
		extractionRequirementsBuilder.requireNestedObjects( paths );
	}

	public void requireStoredField(String absoluteFieldPath, String nestedDocumentPath) {
		extractionRequirementsBuilder.requireStoredField( absoluteFieldPath, nestedDocumentPath );
	}

	public void requireScore() {
		extractionRequirementsBuilder.requireScore();
	}

	public void checkValidField(String absoluteFieldPath) {
		if ( !FieldPaths.isStrictPrefix( absoluteCurrentNestedFieldPath, absoluteFieldPath ) ) {
			throw QueryLog.INSTANCE.invalidContextForProjectionOnField( absoluteFieldPath, absoluteCurrentNestedFieldPath );
		}
	}

	void checkNotNested(SearchQueryElementTypeKey<?> projectionKey, String hint) {
		if ( absoluteCurrentFieldPath() != null ) {
			throw QueryLog.INSTANCE.cannotUseProjectionInNestedContext(
					projectionKey.toString(),
					hint,
					EventContexts.indexSchemaRoot()
			);
		}
	}

	public ProjectionRequestContext root() {
		return new ProjectionRequestContext( extractionRequirementsBuilder, globalHighlighter, namedHighlighters, parameters );
	}

	public ProjectionRequestContext forField(String absoluteFieldPath, boolean nestedObject) {
		checkValidField( absoluteFieldPath );
		return new ProjectionRequestContext(
				extractionRequirementsBuilder, globalHighlighter, namedHighlighters, parameters,
				absoluteFieldPath, nestedObject ? absoluteFieldPath : absoluteCurrentFieldPath
		);
	}

	public String absoluteCurrentNestedFieldPath() {
		return absoluteCurrentNestedFieldPath;
	}

	public boolean projectionCardinalityCorrectlyAddressed(String requiredContextAbsoluteFieldPath) {
		String absoluteCurrentNestedFieldPath = absoluteCurrentNestedFieldPath();
		return requiredContextAbsoluteFieldPath == null
				|| requiredContextAbsoluteFieldPath.equals( absoluteCurrentNestedFieldPath )
				|| ( absoluteCurrentNestedFieldPath != null
						&& absoluteCurrentNestedFieldPath.startsWith( requiredContextAbsoluteFieldPath + "." ) );

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
				throw QueryLog.INSTANCE.cannotFindHighlighterWithName( name, namedHighlighters.keySet() );
			}
			return highlighter;
		}
	}

	public NamedValues queryParameters() {
		return parameters;
	}
}
