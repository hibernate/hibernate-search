/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.dsl.impl;

import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import org.apache.lucene.document.Document;

final class LuceneDocumentProjectionFinalStep implements ProjectionFinalStep<Document> {
	private final SearchProjectionBuilder<Document> builder;

	LuceneDocumentProjectionFinalStep(LuceneSearchProjectionBuilderFactory factory) {
		this.builder = factory.document();
	}

	@Override
	public SearchProjection<Document> toProjection() {
		return builder.build();
	}
}
