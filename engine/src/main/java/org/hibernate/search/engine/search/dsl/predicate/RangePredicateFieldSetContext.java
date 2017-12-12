/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * @author Yoann Rodiere
 */
public interface RangePredicateFieldSetContext<N> extends MultiFieldPredicateFieldSetContext<RangePredicateFieldSetContext<N>> {

	RangePredicateFromContext<N> from(Object value, RangeBoundInclusion inclusion);

	default RangePredicateFromContext<N> from(Object value) {
		return from( value, RangeBoundInclusion.INCLUDED );
	}

	N above(Object value, RangeBoundInclusion inclusion);

	default N above(Object value) {
		return above( value, RangeBoundInclusion.INCLUDED );
	}

	N below(Object value, RangeBoundInclusion inclusion);

	default N below(Object value) {
		return below( value, RangeBoundInclusion.INCLUDED );
	}

}
