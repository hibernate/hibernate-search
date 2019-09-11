/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

public class ElasticsearchExplanationProjectionFinalStep implements ProjectionFinalStep<String> {
	private final SearchProjectionBuilder<String> builder;

	ElasticsearchExplanationProjectionFinalStep(ElasticsearchSearchProjectionBuilderFactory factory) {
		this.builder = factory.explanation();
	}

	@Override
	public SearchProjection<String> toProjection() {
		return builder.build();
	}
}
