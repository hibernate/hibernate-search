/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneStandardMatchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;

import org.apache.lucene.search.Query;

class LuceneNumericMatchPredicateBuilder<F, E extends Number>
		extends AbstractLuceneStandardMatchPredicateBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {

	LuceneNumericMatchPredicateBuilder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
			AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( searchContext, field, codec );
	}

	@Override
	protected Query doBuild(PredicateRequestContext context) {
		return codec.getDomain().createExactQuery( absoluteFieldPath, value );
	}
}
