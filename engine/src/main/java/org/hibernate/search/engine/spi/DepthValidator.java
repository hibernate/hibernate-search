/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spi;

/**
 * Used to check the constraints of depth when using {@link org.hibernate.search.annotations.IndexedEmbedded}
 * or {@link org.hibernate.search.annotations.ContainedIn} annotations.
 *
 * @author Davide D'Alto
 */
public class DepthValidator {

	private final int maxDepth;
	private int depth;

	public DepthValidator(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public int getDepth() {
		return depth;
	}

	public void increaseDepth() {
		depth++;
	}

	public boolean isMaxDepthReached() {
		return depth > maxDepth;
	}

	public boolean isMaxDepthInfinite() {
		return maxDepth == Integer.MAX_VALUE;
	}

	@Override
	public String toString() {
		return "[maxDepth=" + maxDepth + ", level=" + depth + "]";
	}
}
