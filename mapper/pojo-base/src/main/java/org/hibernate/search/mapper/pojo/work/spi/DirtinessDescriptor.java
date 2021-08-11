/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.io.Serializable;
import java.util.Set;

public final class DirtinessDescriptor implements Serializable {

	private final boolean forceSelfDirty;
	private final boolean forceContainingDirty;
	private final Set<String> dirtyPaths;
	private final boolean updateBecauseOfContained;

	public DirtinessDescriptor(boolean forceSelfDirty, boolean forceContainingDirty,
			Set<String> dirtyPaths, boolean updatedBecauseOfContained) {
		this.forceSelfDirty = forceSelfDirty;
		this.forceContainingDirty = forceContainingDirty;
		this.dirtyPaths = dirtyPaths;
		this.updateBecauseOfContained = updatedBecauseOfContained;
	}

	@Override
	public String toString() {
		return "UpdateCauseDescriptor{" +
				"forceSelfDirty=" + forceSelfDirty +
				", forceContainingDirty=" + forceContainingDirty +
				", dirtyPaths=" + dirtyPaths +
				", updatedBecauseOfContained=" + updateBecauseOfContained +
				'}';
	}

	public boolean forceSelfDirty() {
		return forceSelfDirty;
	}

	public boolean forceContainingDirty() {
		return forceContainingDirty;
	}

	public Set<String> dirtyPaths() {
		return dirtyPaths;
	}

	public boolean updatedBecauseOfContained() {
		return updateBecauseOfContained;
	}
}
