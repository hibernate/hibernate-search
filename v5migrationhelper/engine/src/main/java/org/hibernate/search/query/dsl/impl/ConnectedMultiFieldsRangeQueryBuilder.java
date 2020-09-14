/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.RangePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ConnectedMultiFieldsRangeQueryBuilder
		extends AbstractConnectedMultiFieldsQueryBuilder<RangeTerminationExcludable, RangePredicateOptionsStep<?>>
		implements RangeTerminationExcludable {
	private final RangeQueryContext rangeContext;

	public ConnectedMultiFieldsRangeQueryBuilder(QueryBuildingContext queryContext, QueryCustomizer queryCustomizer,
			FieldsContext fieldsContext, RangeQueryContext rangeContext) {
		super( queryContext, queryCustomizer, fieldsContext );
		this.rangeContext = rangeContext;
	}

	@Override
	public RangeTerminationExcludable excludeLimit() {
		if ( rangeContext.getFrom() != null && rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else if ( rangeContext.getFrom() != null ) {
			rangeContext.setExcludeFrom( true );
		}
		else if ( rangeContext.getTo() != null ) {
			rangeContext.setExcludeTo( true );
		}
		else {
			throw new AssertionFailure( "Both from and to clause of a range query are null" );
		}
		return this;
	}

	@Override
	protected RangePredicateOptionsStep<?> createPredicate(SearchPredicateFactory factory, FieldContext fieldContext) {
		return fieldContext.applyBoost( factory.range().field( fieldContext.getField() ) )
				.range(
						Range.between(
								rangeContext.getFrom(),
								rangeContext.isExcludeFrom() ? RangeBoundInclusion.EXCLUDED : RangeBoundInclusion.INCLUDED,
								rangeContext.getTo(),
								rangeContext.isExcludeTo() ? RangeBoundInclusion.EXCLUDED : RangeBoundInclusion.INCLUDED
						),
						fieldContext.getValueConvert()
				);
	}

}
