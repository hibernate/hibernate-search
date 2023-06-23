/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.RangeMatchingContext;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedRangeMatchingContext implements RangeMatchingContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final RangeQueryContext rangeContext;
	private final FieldsContext fieldsContext;

	public ConnectedRangeMatchingContext(String fieldName,
			QueryCustomizer queryCustomizer,
			QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.rangeContext = new RangeQueryContext();
		this.fieldsContext = new FieldsContext( new String[] { fieldName }, queryContext );
	}

	@Override
	public RangeMatchingContext andField(String field) {
		this.fieldsContext.add( field );
		return this;
	}

	@Override
	public <T> FromRangeContext<T> from(T from) {
		rangeContext.setFrom( from );
		return new ConnectedFromRangeContext<T>( this );
	}

	static class ConnectedFromRangeContext<T> implements FromRangeContext<T> {
		private final ConnectedRangeMatchingContext mother;

		ConnectedFromRangeContext(ConnectedRangeMatchingContext mother) {
			this.mother = mother;
		}

		@Override
		public RangeTerminationExcludable to(T to) {
			mother.rangeContext.setTo( to );
			return new ConnectedMultiFieldsRangeQueryBuilder(
					mother.queryContext,
					mother.queryCustomizer,
					mother.fieldsContext,
					mother.rangeContext
			);
		}

		@Override
		public FromRangeContext<T> excludeLimit() {
			mother.rangeContext.setExcludeFrom( true );
			return this;
		}
	}

	@Override
	public RangeTerminationExcludable below(Object below) {
		rangeContext.setTo( below );
		return new ConnectedMultiFieldsRangeQueryBuilder( queryContext, queryCustomizer, fieldsContext, rangeContext );
	}

	@Override
	public RangeTerminationExcludable above(Object above) {
		rangeContext.setFrom( above );
		return new ConnectedMultiFieldsRangeQueryBuilder( queryContext, queryCustomizer, fieldsContext, rangeContext );
	}

	@Override
	public RangeMatchingContext boostedTo(float boost) {
		fieldsContext.boostedTo( boost );
		return this;
	}

	@Override
	public RangeMatchingContext ignoreAnalyzer() {
		fieldsContext.ignoreAnalyzer();
		return this;
	}

	@Override
	public RangeMatchingContext ignoreFieldBridge() {
		fieldsContext.ignoreFieldBridge();
		return this;
	}
}
