/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.facet.RangeFacet;

/**
 * @author Hardy Ferentschik
 */
public class RangeFacetImpl<T> extends AbstractFacet implements RangeFacet<T> {
	/**
	 * The facet range, speak the min and max values for this range facet
	 */
	private final FacetRange<T> range;

	RangeFacetImpl(String facetingName, String absoluteFieldPath, FacetRange<T> range, int count) {
		super( facetingName, absoluteFieldPath, range.getRangeString(), count );
		this.range = range;
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

}
