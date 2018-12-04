/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.CollectionHelper;

final class CompositeBeanHolder<T> implements BeanHolder<List<T>> {

	private final List<? extends BeanHolder<? extends T>> delegates;
	private final List<T> instances;

	CompositeBeanHolder(List<? extends BeanHolder<? extends T>> delegates) {
		this.delegates = delegates;
		List<T> tmp = new ArrayList<>( delegates.size() );
		for ( BeanHolder<? extends T> delegate : delegates ) {
			tmp.add( delegate.get() );
		}
		this.instances = CollectionHelper.toImmutableList( tmp );
	}

	@Override
	public List<T> get() {
		return instances;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( BeanHolder::close, delegates );
		}
	}
}
