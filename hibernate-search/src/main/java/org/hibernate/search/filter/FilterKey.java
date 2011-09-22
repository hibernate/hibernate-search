/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

	public abstract int hashCode();

	public abstract boolean equals(Object obj);
}
