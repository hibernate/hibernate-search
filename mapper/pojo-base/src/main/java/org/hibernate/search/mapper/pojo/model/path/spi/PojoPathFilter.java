/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

import java.util.BitSet;

/**
 * Defines a set of paths that are of importance,
 * so that they can be detected at runtime when given a set of paths.
 * <p>
 * Used in particular in dirty checking,
 * see {@link org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver}.
 */
public interface PojoPathFilter {

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
	boolean test(BitSet pathSelection);

	/**
	 * Sets the ordinal corresponding to the given path in a bitset if the path is accepted by the filter,
	 * and return that bitset or {@code null} if the path is not accepted by the filter.
	 *
	 * @param path The string representations of a path. Never {@code null}.
	 * @return A {@link BitSet} whose only set bit is the ordinal of the given path if the path is accepted by this filter,
	 * or {@code null} if the path is not accepted by this filter.
	 */
	BitSet filter(String path);

	/**
	 * For each path in the given array,
	 * sets the corresponding ordinal in a bitset if the path is accepted by the filter,
	 * and return that bitset or {@code null} if none of the paths are relevant.
	 *
	 * @param paths A array of string representations of paths. Never {@code null}.
	 * @return A {@link BitSet} representing all paths that are included in {@code paths} and are accepted by this filter.
	 * {@code null} if none of the paths is accepted by this filter.
	 */
	BitSet filter(String... paths);

	/**
	 * For each path ordinal in the given array,
	 * sets the corresponding ordinal in a bitset if the path is accepted by the filter,
	 * and return that bitset or {@code null} if none of the paths are relevant.
	 *
	 * @param pathOrdinals A array of path ordinals. Never {@code null}.
	 * @return A {@link BitSet} representing all paths that are included in {@code paths} and are accepted by this filter.
	 * {@code null} if none of the paths is accepted by this filter.
	 */
	BitSet filter(int[] pathOrdinals);

	/**
	 * For the given path ordinal,
	 * sets the corresponding ordinal in a bitset if the path is accepted by the filter,
	 * and return that bitset or {@code null} if the path is not relevant.
	 *
	 * @param pathOrdinal A path ordinal.
	 * @return A {@link BitSet} representing the single given path if it is accepted by this filter,
	 * or {@code null} if it is not.
	 */
	BitSet filter(int pathOrdinal);

	/**
	 * @return A {@link BitSet} representing all paths that are accepted by this filter, or {@code null} if there is none.
	 */
	BitSet all();

}
