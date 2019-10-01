/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import java.util.Set;

/**
 * A filter expecting a simple string representation of dirty paths.
 * <p>
 * Completely ignores container value extractors.
 */
public final class StringSetPojoPathFilter implements PojoPathFilter<Set<String>> {

	private final Set<String> acceptedPaths;

	public StringSetPojoPathFilter(Set<String> acceptedPaths) {
		this.acceptedPaths = acceptedPaths;
	}

	@Override
	public boolean test(Set<String> paths) {
		Set<String> iterationSet = acceptedPaths;
		Set<String> containsSet = paths;

		// Try to limit the amount of iteration
		if ( paths.size() < acceptedPaths.size() ) {
			iterationSet = paths;
			containsSet = acceptedPaths;
		}

		// Return true if the sets have at least one path in common
		for ( String path : iterationSet ) {
			if ( containsSet.contains( path ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + acceptedPaths + "]";
	}
}
