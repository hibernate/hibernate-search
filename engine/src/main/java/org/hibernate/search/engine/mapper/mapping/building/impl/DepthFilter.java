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

	private final Integer maxDepth;

	private DepthFilter(Integer maxDepth) {
		this.maxDepth = maxDepth;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "maxDepth=" + maxDepth
				+ "]";
	}

	boolean isEveryPathIncludedAtDepth(int relativeDepth) {
		return maxDepth == null || maxDepth > relativeDepth;
	}

	boolean hasDepthLimit() {
		return maxDepth != null;
	}

}
