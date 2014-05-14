/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter;

/**
 * The key object must implement equals / hashcode so that 2 keys are equals if and only if
 * the given Filter types are the same and the set of parameters are the same.
 * <p/>
 * The FilterKey creator (ie the @Key method) does not have to inject <code>impl</code>
 * It will be done by Hibernate Search
 *
 * @author Emmanuel Bernard
 */
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
