/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

/**
 * An abstract base for implementations of a {@link PojoTypeModel}
 * representing a type with generic type parameters.
 */
public abstract class AbstractPojoGenericTypeModel<T> implements PojoTypeModel<T> {

	private final PojoRawTypeModel<? super T> rawTypeModel;

	protected AbstractPojoGenericTypeModel(PojoRawTypeModel<? super T> rawTypeModel) {
		this.rawTypeModel = rawTypeModel;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[" + name() + "]";
	}

	@Override
	public final PojoRawTypeModel<? super T> rawType() {
		return rawTypeModel;
	}

	@Override
	public PojoPropertyModel<?> property(String propertyName) {
		return rawTypeModel.property( propertyName );
	}

}
