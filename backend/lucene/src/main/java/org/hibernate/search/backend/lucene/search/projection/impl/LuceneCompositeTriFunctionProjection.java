/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;


import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.util.common.function.TriFunction;

class LuceneCompositeTriFunctionProjection<P1, P2, P3, P>
		extends AbstractLuceneCompositeProjection<P> {

	private final TriFunction<P1, P2, P3, P> transformer;

	LuceneCompositeTriFunctionProjection(LuceneSearchContext searchContext, TriFunction<P1, P2, P3, P> transformer,
			LuceneSearchProjection<?, P1> projection1, LuceneSearchProjection<?, P2> projection2,
			LuceneSearchProjection<?, P3> projection3) {
		super( searchContext, projection1, projection2, projection3 );
		this.transformer = transformer;
	}

	@Override
	@SuppressWarnings("unchecked")
	P doTransform(Object[] childResults) {
		return transformer.apply( (P1) childResults[0], (P2) childResults[1], (P3) childResults[2] );
	}
}
