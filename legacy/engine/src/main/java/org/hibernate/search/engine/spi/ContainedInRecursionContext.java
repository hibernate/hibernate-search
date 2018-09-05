/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spi;

import java.util.Set;

/**
 * Used to check the constraints of depth when using {@link org.hibernate.search.annotations.IndexedEmbedded}
 * or {@link org.hibernate.search.annotations.ContainedIn} annotations.
 *
 * @author Davide D'Alto
 * @author Yoann Rodiere
 */
public class ContainedInRecursionContext {

	private final int maxDepth;
	private final int depth;
	private final Set<String> comprehensivePaths;

	public ContainedInRecursionContext(int maxDepth, int depth, Set<String> comprehensivePaths) {
		this.maxDepth = maxDepth;
		this.depth = depth;
		this.comprehensivePaths = comprehensivePaths;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public int getDepth() {
		return depth;
	}

	public Set<String> getComprehensivePaths() {
		return comprehensivePaths;
	}

	public boolean isTerminal() {
		return depth > maxDepth || comprehensivePaths != null && comprehensivePaths.isEmpty();
	}

	@Override
	public String toString() {
		return "[maxDepth=" + maxDepth + ", level=" + depth + ", comprehensivePaths=" + comprehensivePaths + "]";
	}
}
