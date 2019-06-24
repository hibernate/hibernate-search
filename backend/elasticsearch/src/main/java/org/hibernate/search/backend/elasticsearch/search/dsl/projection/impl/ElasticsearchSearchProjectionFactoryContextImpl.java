/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.projection.ElasticsearchSearchProjectionFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.dsl.projection.spi.DelegatingSearchProjectionFactoryContext;

public class ElasticsearchSearchProjectionFactoryContextImpl<R, E>
		extends DelegatingSearchProjectionFactoryContext<R, E>
		implements ElasticsearchSearchProjectionFactoryContext<R, E> {

	private final ElasticsearchSearchProjectionBuilderFactory factory;

	public ElasticsearchSearchProjectionFactoryContextImpl(SearchProjectionFactoryContext<R, E> delegate,
			ElasticsearchSearchProjectionBuilderFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public ProjectionFinalStep<String> source() {
		return new ElasticsearchSourceProjectionFinalStep( factory );
	}

	@Override
	public ProjectionFinalStep<String> explanation() {
		return new ElasticsearchExplanationProjectionFinalStep( factory );
	}
}
