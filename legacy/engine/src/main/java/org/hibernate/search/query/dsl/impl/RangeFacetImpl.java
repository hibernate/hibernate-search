/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;


import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
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

	RangeFacetImpl(String facetingName, String facetFieldName, String sourceFieldName, FacetRange<T> range, int count, int index) {
		super( facetingName, facetFieldName, sourceFieldName, range.getRangeString(), count );
		this.range = range;
		this.rangeIndex = index;
	}

	@Override
	public Query getFacetQuery() {
		Object minOrMax = getNonNullMinOrMax( range );
		if ( NumericFieldUtils.requiresNumericRangeQuery( minOrMax ) ) {
			return NumericFieldUtils.createNumericRangeQuery(
					getSourceFieldName(),
					range.getMin(),
					range.getMax(),
					range.isMinIncluded(),
					range.isMaxIncluded()
			);
		}
		else if ( minOrMax instanceof String ) {
			return TermRangeQuery.newStringRange(
					getSourceFieldName(),
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

}
