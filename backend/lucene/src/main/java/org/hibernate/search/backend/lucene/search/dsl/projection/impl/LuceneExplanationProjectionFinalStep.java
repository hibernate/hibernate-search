/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.projection.impl;

import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import org.apache.lucene.search.Explanation;

final class LuceneExplanationProjectionFinalStep implements ProjectionFinalStep<Explanation> {
	private final SearchProjectionBuilder<Explanation> builder;

	LuceneExplanationProjectionFinalStep(LuceneSearchProjectionBuilderFactory factory) {
		this.builder = factory.explanation();
	}

	@Override
	public SearchProjection<Explanation> toProjection() {
		return builder.build();
	}
}
