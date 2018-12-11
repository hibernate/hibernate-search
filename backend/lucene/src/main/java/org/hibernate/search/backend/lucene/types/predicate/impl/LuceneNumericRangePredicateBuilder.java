/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.search.Query;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneStandardRangePredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneNumericDomain;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

class LuceneNumericRangePredicateBuilder<F, E>
		extends AbstractLuceneStandardRangePredicateBuilder<F, E, LuceneNumericFieldCodec<F, E>> {

	LuceneNumericRangePredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends F> converter,
			LuceneNumericFieldCodec<F, E> codec) {
		super( searchContext, absoluteFieldPath, converter, codec );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		LuceneNumericDomain<E> domain = codec.getDomain();
		return domain.createRangeQuery(
				absoluteFieldPath,
				getLowerValue( domain, lowerLimit, excludeLowerLimit ),
				getUpperValue( domain, upperLimit, excludeUpperLimit )
		);
	}

	private static <E> E getLowerValue(LuceneNumericDomain<E> domain, E lowerLimit, boolean excludeLowerLimit) {
		if ( lowerLimit == null ) {
			return domain.getMinValue();
		}
		else {
			return excludeLowerLimit ? domain.getNextValue( lowerLimit ) : lowerLimit;
		}
	}

	private static <E> E getUpperValue(LuceneNumericDomain<E> domain, E upperLimit, boolean excludeUpperLimit) {
		if ( upperLimit == null ) {
			return domain.getMaxValue();
		}
		else {
			return excludeUpperLimit ? domain.getPreviousValue( upperLimit ) : upperLimit;
		}
	}
}
