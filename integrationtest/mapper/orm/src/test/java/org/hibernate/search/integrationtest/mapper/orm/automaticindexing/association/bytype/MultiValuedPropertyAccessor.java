/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class MultiValuedPropertyAccessor<R, V, C> implements PropertyAccessor<R, V> {

	private final Function<R, C> getContainerMethod;
	private final BiConsumer<R, C> setContainerMethod;
	private final ContainerPrimitives<C, V> containerPrimitives;

	public MultiValuedPropertyAccessor(ContainerPrimitives<C, V> containerPrimitives, Function<R, C> getContainerMethod) {
		this( containerPrimitives, getContainerMethod, null );
	}

	public MultiValuedPropertyAccessor(ContainerPrimitives<C, V> containerPrimitives, Function<R, C> getContainerMethod,
			BiConsumer<R, C> setContainerMethod) {
		this.getContainerMethod = getContainerMethod;
		this.setContainerMethod = setContainerMethod;
		this.containerPrimitives = containerPrimitives;
	}

	@Override
	public void set(R root, V value) {
		C container = getContainer( root );
		containerPrimitives.clear( container );
		containerPrimitives.add( container, value );
	}

	@Override
	public V get(R root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear(R root) {
		C container = getContainer( root );
		containerPrimitives.clear( container );
	}

	public void add(R root, V value) {
		C container = getContainer( root );
		containerPrimitives.add( container, value );
	}

	public void remove(R root, V value) {
		C container = getContainer( root );
		containerPrimitives.remove( container, value );
	}

	public void setContainer(R root, C container) {
		if ( setContainerMethod == null ) {
			throw new UnsupportedOperationException();
		}
		setContainerMethod.accept( root, container );
	}

	public C getContainer(R root) {
		return getContainerMethod.apply( root );
	}

}
