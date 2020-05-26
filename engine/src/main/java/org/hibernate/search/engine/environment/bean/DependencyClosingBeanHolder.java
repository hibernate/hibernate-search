/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import java.util.List;

import org.hibernate.search.util.common.impl.Closer;

final class DependencyClosingBeanHolder<T> implements BeanHolder<T> {

	private final BeanHolder<T> delegate;
	private final List<BeanHolder<?>> dependencies;

	DependencyClosingBeanHolder(BeanHolder<T> delegate, List<BeanHolder<?>> dependencies) {
		this.delegate = delegate;
		this.dependencies = dependencies;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "delegate=" + delegate
				+ ", dependencies=" + dependencies
				+ "]";
	}

	@Override
	public T get() {
		return delegate.get();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( BeanHolder::close, delegate );
			closer.pushAll( BeanHolder::close, dependencies );
		}
	}
}
