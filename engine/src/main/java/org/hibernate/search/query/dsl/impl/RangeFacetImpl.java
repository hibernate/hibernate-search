/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import java.util.Date;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.facet.RangeFacet;

/**
 * @author Hardy Ferentschik
 */
public class RangeFacetImpl<T> extends AbstractFacet implements RangeFacet<T> {
	/**
	 * The facet range, speak the min and max values for this range facet
	 */
	private final FacetRange<T> range;

	/**
	 * The index of the specified ranges
	 */
	private final int rangeIndex;

	RangeFacetImpl(String facetingName, String fieldName, FacetRange<T> range, int count, int index) {
		super( facetingName, fieldName, range.getRangeString(), count );
		this.range = range;
		this.rangeIndex = index;
	}

	@Override
	public Query getFacetQuery() {
		Object minOrMax = getNonNullMinOrMax( range );
		if ( minOrMax instanceof Number || minOrMax instanceof Date ) {
			return createNumericRangeQuery();
		}
		else if ( minOrMax instanceof String ) {
			return createRangeQuery(
					(String) range.getMin(),
					(String) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else {
			throw new AssertionFailure( "Unsupported range type" );
		}
	}

	public int getRangeIndex() {
		return rangeIndex;
	}

	@Override
	public T getMin() {
		return range.getMin();
	}

	@Override
	public T getMax() {
		return range.getMax();
	}

	@Override
	public boolean isIncludeMin() {
		return range.isMinIncluded();
	}

	@Override
	public boolean isIncludeMax() {
		return range.isMaxIncluded();
	}

	private Object getNonNullMinOrMax(FacetRange<T> range) {
		Object o = range.getMin();
		if ( o == null ) {
			o = range.getMax();
		}
		return o;
	}

	private Query createNumericRangeQuery() {
		NumericRangeQuery query;
		// either end of the range must have a valid value (see also HSEARCH-770)
		Object minOrMax = getNonNullMinOrMax( range );
		if ( minOrMax instanceof Double ) {
			query = NumericRangeQuery.newDoubleRange(
					getFieldName(),
					(Double) range.getMin(),
					(Double) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else if ( minOrMax instanceof Float ) {
			query = NumericRangeQuery.newFloatRange(
					getFieldName(),
					(Float) range.getMin(),
					(Float) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else if ( minOrMax instanceof Integer ) {
			query = NumericRangeQuery.newIntRange(
					getFieldName(),
					(Integer) range.getMin(),
					(Integer) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else if ( minOrMax instanceof Long ) {
			query = NumericRangeQuery.newLongRange(
					getFieldName(),
					(Long) range.getMin(),
					(Long) range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else if ( minOrMax instanceof Date ) {
			query = NumericRangeQuery.newLongRange(
					getFieldName(),
					range.getMin() == null ? null : ( (Date) range.getMin() ).getTime(),
					range.getMax() == null ? null : ( (Date) range.getMax() ).getTime(),

					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else {
			throw new AssertionFailure( "Unsupported range type" );
		}
		return query;
	}

	private Query createRangeQuery(String min, String max, boolean includeMin, boolean includeMax) {
		return TermRangeQuery.newStringRange( getFieldName(), min, max, includeMin, includeMax );
	}

}
