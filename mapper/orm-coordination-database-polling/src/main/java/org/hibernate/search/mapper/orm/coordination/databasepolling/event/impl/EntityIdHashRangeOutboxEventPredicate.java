/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl;

import java.util.Optional;

import org.hibernate.query.Query;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

public final class EntityIdHashRangeOutboxEventPredicate implements OutboxEventPredicate {
	private static final String LOWER_BOUND_PARAM_NAME = "lowerHash";
	private static final String UPPER_BOUND_PARAM_NAME = "upperHash";

	private final Range<Integer> range;

	public EntityIdHashRangeOutboxEventPredicate(Range<Integer> range) {
		this.range = range;
	}

	@Override
	public String queryPart(String eventAlias) {
		StringBuilder builder = new StringBuilder( eventAlias );
		builder.append( ".entityIdHash " );
		RangeBoundInclusion lowerBoundCondition = range.lowerBoundValue().isPresent() ? range.lowerBoundInclusion() : null;
		RangeBoundInclusion upperBoundCondition = range.upperBoundValue().isPresent() ? range.upperBoundInclusion() : null;

		if ( lowerBoundCondition == RangeBoundInclusion.INCLUDED
				&& upperBoundCondition == RangeBoundInclusion.INCLUDED ) {
			builder.append( "between :" ).append( LOWER_BOUND_PARAM_NAME )
					.append( " and :" ).append( UPPER_BOUND_PARAM_NAME );
		}
		else {
			if ( lowerBoundCondition == RangeBoundInclusion.INCLUDED ) {
				builder.append( " >= :" ).append( LOWER_BOUND_PARAM_NAME );
			}
			else if ( lowerBoundCondition == RangeBoundInclusion.EXCLUDED ) {
				builder.append( " > :" ).append( LOWER_BOUND_PARAM_NAME );
			}
			if ( lowerBoundCondition != null && upperBoundCondition != null ) {
				builder.append( " and " ).append( eventAlias ).append( ".entityIdHash " );
			}
			if ( upperBoundCondition == RangeBoundInclusion.INCLUDED ) {
				builder.append( " <= :" ).append( UPPER_BOUND_PARAM_NAME );
			}
			else if ( upperBoundCondition == RangeBoundInclusion.EXCLUDED ) {
				builder.append( " < :" ).append( UPPER_BOUND_PARAM_NAME );
			}
		}
		return builder.toString();
	}

	@Override
	public void setParams(Query<OutboxEvent> query) {
		Optional<Integer> lowerBound = range.lowerBoundValue();
		if ( lowerBound.isPresent() ) {
			query.setParameter( LOWER_BOUND_PARAM_NAME, lowerBound.get() );
		}
		Optional<Integer> upperBound = range.upperBoundValue();
		if ( upperBound.isPresent() ) {
			query.setParameter( UPPER_BOUND_PARAM_NAME, upperBound.get() );
		}
	}
}
