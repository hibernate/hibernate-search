/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.impl;

import java.util.BitSet;
import java.util.stream.Collectors;

/**
 * Defines a set of paths that are of importance,
 * so that they can be detected at runtime when given a set of paths.
 * <p>
 * Used in particular in dirty checking,
 * see {@link org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver}.
 */
public final class PojoPathFilter {

	private final PojoPathOrdinals ordinals;
	private final BitSet acceptedPaths;

	public PojoPathFilter(PojoPathOrdinals ordinals, BitSet acceptedPaths) {
		this.ordinals = ordinals;
		this.acceptedPaths = acceptedPaths;
	}

	@Override
	public String toString() {
		return toString( acceptedPaths );
	}

	/**
	 * For debugging: turns a path selection into a string.
	 * @param pathSelection A {@link BitSet} returned by one of the {@code filter} methods.
	 * @return A string representation of {@code pathSelection}.
	 */
	public String toString(BitSet pathSelection) {
		return "{"
				+ pathSelection.stream().mapToObj( ordinals::toPath )
				.collect( Collectors.joining( ", " ) )
				+ "}";
	}

	/**
	 * Determines if any path in the given set of paths of is accepted by this filter.
	 * <p>
	 * This method is optimized to be called very often.
	 *
	 * @param pathSelection A {@link BitSet} representing a set of paths using the path ordinals.
	 * Never {@code null}.
	 * @return {@code true} if any path in the given set is accepted by this filter,
	 * {@code false} otherwise.
	 */
	public boolean test(BitSet pathSelection) {
		return acceptedPaths.intersects( pathSelection );
	}

	/**
	 * For each path in the given array, sets the corresponding ordinal in the given bitset if it is accepted by the filter.
	 *
	 * @param bitSet A {@link BitSet}. Never {@code null}.
	 * @param paths A array of string representations of paths. Never {@code null}.
	 */
	public void setAccepted(BitSet bitSet, String ... paths) {
		for ( String path : paths ) {
			Integer ordinal = ordinals.toOrdinal( path );
			if ( ordinal == null || !acceptedPaths.get( ordinal ) ) {
				continue;
			}
			bitSet.set( ordinal );
		}
	}

}
