/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.tree.impl;

public final class DepthFilter {

	private static final DepthFilter UNCONSTRAINED = new DepthFilter( null );

	static DepthFilter unconstrained() {
		return UNCONSTRAINED;
	}

	static DepthFilter of(Integer includeDepth) {
		return new DepthFilter( includeDepth );
	}

	private final Integer includeDepth;

	private DepthFilter(Integer includeDepth) {
		this.includeDepth = includeDepth;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "includeDepth=" + includeDepth
				+ "]";
	}

	boolean isEveryPathIncludedAtDepth(int relativeDepth) {
		return includeDepth == null || includeDepth > relativeDepth;
	}

	boolean hasDepthLimit() {
		return includeDepth != null;
	}

}
