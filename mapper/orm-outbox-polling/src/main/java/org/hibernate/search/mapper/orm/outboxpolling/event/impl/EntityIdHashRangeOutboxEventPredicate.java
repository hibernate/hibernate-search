/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.util.Optional;

import org.hibernate.query.Query;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

public final class EntityIdHashRangeOutboxEventPredicate implements OutboxEventPredicate {
	private static final String LOWER_BOUND_PARAM_NAME = "lowerHash";
	private static final String UPPER_BOUND_PARAM_NAME = "upperHash";

	private final Integer lowerBoundIncluded;
	private final Integer upperBoundIncluded;

	public EntityIdHashRangeOutboxEventPredicate(Range<Integer> range) {
		Optional<Integer> lowerBound = range.lowerBoundValue();
		if ( lowerBound.isPresent() ) {
			Integer lowerBoundValue = lowerBound.get();
			if ( range.lowerBoundInclusion() == RangeBoundInclusion.EXCLUDED ) {
				++lowerBoundValue;
			}
			lowerBoundIncluded = lowerBoundValue;
		}
		else {
			lowerBoundIncluded = null;
		}
		Optional<Integer> upperBound = range.upperBoundValue();
		if ( upperBound.isPresent() ) {
			Integer upperBoundValue = upperBound.get();
			if ( range.upperBoundInclusion() == RangeBoundInclusion.EXCLUDED ) {
				--upperBoundValue;
			}
			upperBoundIncluded = upperBoundValue;
		}
		else {
			upperBoundIncluded = null;
		}
	}

	@Override
	public String queryPart(String eventAlias) {
		StringBuilder builder = new StringBuilder( eventAlias );
		builder.append( ".entityIdHash " );

		// We try to use a single predicate instead of "... <= ... and ... < ...",
		// because this seems to help SQL Server come up with better query plans.
		// This forces us to adjust the value of bounds so that they are included
		// (because the BETWEEN predicate includes both bounds),
		// but since we're dealing with integers that's easy.
		if ( lowerBoundIncluded != null && upperBoundIncluded != null ) {
			builder.append( "between :" ).append( LOWER_BOUND_PARAM_NAME )
					.append( " and :" ).append( UPPER_BOUND_PARAM_NAME );
		}
		else if ( lowerBoundIncluded != null ) {
			builder.append( " >= :" ).append( LOWER_BOUND_PARAM_NAME );
		}
		else if ( upperBoundIncluded != null ) {
			builder.append( " <= :" ).append( UPPER_BOUND_PARAM_NAME );
		}
		return builder.toString();
	}

	@Override
	public void setParams(Query<?> query) {
		if ( lowerBoundIncluded != null ) {
			query.setParameter( LOWER_BOUND_PARAM_NAME, lowerBoundIncluded );
		}
		if ( upperBoundIncluded != null ) {
			query.setParameter( UPPER_BOUND_PARAM_NAME, upperBoundIncluded );
		}
	}
}
