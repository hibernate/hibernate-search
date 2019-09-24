/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

class DepthFilter {

	private static final DepthFilter UNCONSTRAINED = new DepthFilter( null );

	static DepthFilter unconstrained() {
		return UNCONSTRAINED;
	}

	static DepthFilter of(Integer maxDepth) {
		return new DepthFilter( maxDepth );
	}

	private final Integer remainingDepth;

	private DepthFilter(Integer remainingDepth) {
		this.remainingDepth = remainingDepth;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "remainingDepth=" + remainingDepth
				+ "]";
	}

	boolean hasDepthRemaining() {
		return remainingDepth == null || remainingDepth > 0;
	}

	boolean hasDepthLimit() {
		return remainingDepth != null;
	}

	public DepthFilter increaseDepth() {
		return new DepthFilter( remainingDepth == null ? null : remainingDepth - 1 );
	}

	public DepthFilter combine(DepthFilter otherFilter) {
		Integer composedRemainingDepth = remainingDepth;
		// Determine the minimum depth (null is highest)
		if ( remainingDepth == null
				|| otherFilter.remainingDepth != null && remainingDepth > otherFilter.remainingDepth ) {
			composedRemainingDepth = otherFilter.remainingDepth;
		}

		return new DepthFilter( composedRemainingDepth );
	}
}
