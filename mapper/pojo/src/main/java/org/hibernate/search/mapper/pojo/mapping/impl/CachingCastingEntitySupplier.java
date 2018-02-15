/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public class CachingCastingEntitySupplier<E> implements Supplier<E> {

	private final PojoTypeModel<E> typeModel;
	private final PojoProxyIntrospector proxyIntrospector;
	private final Object potentiallyProxiedEntity;

	private E unproxiedEntity;

	public CachingCastingEntitySupplier(
			PojoTypeModel<E> typeModel,
			PojoProxyIntrospector proxyIntrospector,
			Object potentiallyProxiedEntity) {
		this.typeModel = typeModel;
		this.proxyIntrospector = proxyIntrospector;
		this.potentiallyProxiedEntity = potentiallyProxiedEntity;
	}

	@Override
	public E get() {
		if ( unproxiedEntity == null ) {
			unproxiedEntity = typeModel.cast( proxyIntrospector.unproxy( potentiallyProxiedEntity ) );
		}
		return unproxiedEntity;
	}
}
