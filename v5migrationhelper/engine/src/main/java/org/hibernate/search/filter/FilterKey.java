/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter;

/**
 * @deprecated Custom filter keys are deprecated and are scheduled for removal in Hibernate Search 6. As of Hibernate
 * Search 5.1, keys for caching Lucene filters are calculated automatically based on the given filter parameters.
 */
@Deprecated
public abstract class FilterKey {
	// FilterKey implementations do not have to be thread-safe as FilterCachingStrategy ensure
	// a memory barrier between usages
	private Class<?> impl;

	/**
	 * @return the {@code @FullTextFilterDef.impl} class
	 */
	public Class<?> getImpl() {
		return impl;
	}

	public void setImpl(Class<?> impl) {
		this.impl = impl;
	}

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);
}
