/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneStandardRangePredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.apache.lucene.search.Query;

class LuceneNumericRangePredicateBuilder<F, E extends Number>
		extends AbstractLuceneStandardRangePredicateBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {

	LuceneNumericRangePredicateBuilder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
			AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( searchContext, field, codec );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		LuceneNumericDomain<E> domain = codec.getDomain();
		return domain.createRangeQuery(
				absoluteFieldPath,
				getLowerValue( domain, range.lowerBoundValue(), range.lowerBoundInclusion() ),
				getUpperValue( domain, range.upperBoundValue(), range.upperBoundInclusion() )
		);
	}

	private static <E extends Number> E getLowerValue(LuceneNumericDomain<E> domain, Optional<E> boundValueOptional,
			RangeBoundInclusion inclusion) {
		if ( !boundValueOptional.isPresent() ) {
			return domain.getMinValue();
		}
		E boundValue = boundValueOptional.get();
		return RangeBoundInclusion.EXCLUDED.equals( inclusion ) ? domain.getNextValue( boundValue ) : boundValue;
	}

	private static <E extends Number> E getUpperValue(LuceneNumericDomain<E> domain, Optional<E> boundValueOptional,
			RangeBoundInclusion inclusion) {
		if ( !boundValueOptional.isPresent() ) {
			return domain.getMaxValue();
		}
		E boundValue = boundValueOptional.get();
		return RangeBoundInclusion.EXCLUDED.equals( inclusion ) ? domain.getPreviousValue( boundValue ) : boundValue;
	}
}
