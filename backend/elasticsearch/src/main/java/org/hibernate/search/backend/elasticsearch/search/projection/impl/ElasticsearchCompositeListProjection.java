/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ElasticsearchCompositeListProjection<P>
		extends AbstractElasticsearchCompositeProjection<P> {

	private final Function<List<?>, P> transformer;

	public ElasticsearchCompositeListProjection(Set<String> indexNames, Function<List<?>, P> transformer,
			List<ElasticsearchSearchProjection<?, ?>> children) {
		super( indexNames, children.toArray( new ElasticsearchSearchProjection<?, ?>[0] ) );
		this.transformer = transformer;
	}

	@Override
	P doTransform(Object[] childResults) {
		return transformer.apply( Arrays.asList( childResults ) );
	}

}
