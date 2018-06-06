/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.impl;

import java.util.Set;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

public interface PojoDirtinessState {

	/**
	 * Determines whether any path in a given set of paths is considered dirty.
	 * <p>
	 * This method may be called very often. Implementations should take care to
	 * organize their internal data adequately, so that lookups are fast.
	 *
	 * @param paths The set of paths to test for dirtiness.
	 * The set must be non-null and non-empty, and the elements must be non-null.
	 * @return {@code true} if any of the given properties is considered dirty, {@code false otherwise}.
	 */
	boolean isAnyDirty(Set<PojoModelPathValueNode> paths);

	static PojoDirtinessState allDirty() {
		return ignored -> true;
	}

}
