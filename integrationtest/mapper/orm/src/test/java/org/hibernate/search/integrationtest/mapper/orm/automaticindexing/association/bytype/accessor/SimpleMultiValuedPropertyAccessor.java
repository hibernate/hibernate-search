/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;

final class SimpleMultiValuedPropertyAccessor<R, V, C>
		implements MultiValuedPropertyAccessor<R, V, C> {

	private final Function<R, C> getContainerMethod;
	private final BiConsumer<R, C> setContainerMethod;
	private final ContainerPrimitives<C, V> containerPrimitives;

	SimpleMultiValuedPropertyAccessor(ContainerPrimitives<C, V> containerPrimitives,
			Function<R, C> getContainerMethod) {
		this( containerPrimitives, getContainerMethod, null );
	}

	SimpleMultiValuedPropertyAccessor(ContainerPrimitives<C, V> containerPrimitives,
			Function<R, C> getContainerMethod,
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
		C container = getContainer( root );
		Iterator<V> iterator = containerPrimitives.iterator( container );
		if ( !iterator.hasNext() ) {
			return null;
		}
		V first = iterator.next();
		if ( iterator.hasNext() ) {
			throw new IllegalArgumentException(
					"get() can only be used if the container is empty or contains a single value, but the container currently contains: "
							+ container );
		}
		return first;
	}

	@Override
	public void clear(R root) {
		C container = getContainer( root );
		containerPrimitives.clear( container );
	}

	@Override
	public void add(R root, V value) {
		C container = getContainer( root );
		containerPrimitives.add( container, value );
	}

	@Override
	public void remove(R root, V value) {
		C container = getContainer( root );
		containerPrimitives.remove( container, value );
	}

	@Override
	public void setContainer(R root, C container) {
		if ( setContainerMethod == null ) {
			throw new UnsupportedOperationException();
		}
		setContainerMethod.accept( root, container );
	}

	@Override
	public C getContainer(R root) {
		return getContainerMethod.apply( root );
	}

}
