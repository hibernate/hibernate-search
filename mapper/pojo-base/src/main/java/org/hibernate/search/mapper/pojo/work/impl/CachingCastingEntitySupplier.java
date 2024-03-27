/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public class CachingCastingEntitySupplier<E> implements Supplier<E> {

	private final PojoCaster<E> caster;
	private final PojoRuntimeIntrospector proxyIntrospector;
	private final Object potentiallyProxiedEntity;

	private E unproxiedEntity;

	public CachingCastingEntitySupplier(
			PojoCaster<E> caster,
			PojoRuntimeIntrospector proxyIntrospector,
			Object potentiallyProxiedEntity) {
		this.caster = caster;
		this.proxyIntrospector = proxyIntrospector;
		this.potentiallyProxiedEntity = potentiallyProxiedEntity;
	}

	@Override
	public E get() {
		if ( unproxiedEntity == null ) {
			// TODO HSEARCH-3100 avoid unnecessary unproxying by asking the introspector if the entity has the target type first
			unproxiedEntity = caster.cast( proxyIntrospector.unproxy( potentiallyProxiedEntity ) );
		}
		return unproxiedEntity;
	}
}
