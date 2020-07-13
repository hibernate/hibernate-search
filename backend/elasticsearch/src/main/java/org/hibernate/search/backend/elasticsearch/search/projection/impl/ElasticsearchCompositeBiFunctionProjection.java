/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.function.BiFunction;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;

class ElasticsearchCompositeBiFunctionProjection<P1, P2, P>
		extends AbstractElasticsearchCompositeProjection<P> {

	private final BiFunction<P1, P2, P> transformer;

	ElasticsearchCompositeBiFunctionProjection(ElasticsearchSearchContext searchContext,
			BiFunction<P1, P2, P> transformer,
			ElasticsearchSearchProjection<?, P1> projection1, ElasticsearchSearchProjection<?, P2> projection2) {
		super( searchContext, projection1, projection2 );
		this.transformer = transformer;
	}

	@Override
	@SuppressWarnings("unchecked")
	P doTransform(Object[] childResults) {
		return transformer.apply( (P1) childResults[0], (P2) childResults[1] );
	}
}
