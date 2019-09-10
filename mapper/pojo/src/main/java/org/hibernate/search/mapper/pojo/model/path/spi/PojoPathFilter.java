/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

/**
 * Defines a set of paths that are of importance,
 * so that they can be detected at runtime when given a set of paths.
 * <p>
 * Used in particular in dirty checking,
 * see {@link org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver}.
 *
 * @param <S> The expected type of the object representing a set of paths.
 */
public interface PojoPathFilter<S> {

	/**
	 * Determines if any path in the given set of paths of is accepted by this filter.
	 * <p>
	 * This method may be called very often. Implementations should take care to
	 * organize their internal data adequately, so that lookups are fast.
	 *
	 * @param paths An object representing a set of paths. Never {@code null}.
	 * @return {@code true} if any path in the given set is accepted by this filter,
	 * {@code false} otherwise.
	 */
	boolean test(S paths);

	static <S> PojoPathFilter<S> empty() {
		return EmptyPojoPathFilter.get();
	}

}
