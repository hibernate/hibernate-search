/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor;

import java.util.function.Supplier;

final class ComposedMultiValuedPropertyAccessor<R, V, U, C>
		extends ComposedPropertyAccessor<R, V, U>
		implements MultiValuedPropertyAccessor<R, U, C> {
	private final MultiValuedPropertyAccessor<? super V, U, C> second;

	ComposedMultiValuedPropertyAccessor(PropertyAccessor<R, V> first, Supplier<V> firstDefaultInstanceSupplier,
			MultiValuedPropertyAccessor<? super V, U, C> second) {
		super( first, firstDefaultInstanceSupplier, second );
		this.second = second;
	}

	@Override
	public void add(R root, U value) {
		second.add( getFirstOrCreate( root ), value );
	}

	@Override
	public void remove(R root, U value) {
		second.remove( getFirstOrFail( root ), value );
	}

	@Override
	public void setContainer(R root, C container) {
		second.setContainer( getFirstOrCreate( root ), container );
	}

	@Override
	public C getContainer(R root) {
		V firstInstance = getFirstOrNull( root );
		if ( firstInstance == null ) {
			return null;
		}
		return second.getContainer( firstInstance );
	}
}
