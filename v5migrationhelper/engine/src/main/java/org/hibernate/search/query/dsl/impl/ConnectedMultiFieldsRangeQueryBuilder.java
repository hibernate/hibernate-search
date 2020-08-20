/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ConnectedMultiFieldsRangeQueryBuilder implements RangeTerminationExcludable {
	private final RangeQueryContext rangeContext;
	private final QueryCustomizer queryCustomizer;
	private final FieldsContext fieldsContexts;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsRangeQueryBuilder(RangeQueryContext rangeContext,
			QueryCustomizer queryCustomizer,
			FieldsContext fieldsContexts,
			QueryBuildingContext queryContext) {
		this.rangeContext = rangeContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContexts = fieldsContexts;
		this.queryContext = queryContext;
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
	public Query createQuery() {
		throw new UnsupportedOperationException( "To be implemented through the Search 6 DSL" );
	}

}
