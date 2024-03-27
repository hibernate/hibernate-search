/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

final class GenericContextAwarePojoPropertyModel<T> implements PojoPropertyModel<T> {

	private final PojoPropertyModel<? super T> rawPropertyModel;
	private final GenericContextAwarePojoGenericTypeModel<T> genericPropertyTypeModel;

	GenericContextAwarePojoPropertyModel(
			PojoPropertyModel<? super T> rawPropertyModel,
			GenericContextAwarePojoGenericTypeModel<T> genericPropertyTypeModel) {
		this.rawPropertyModel = rawPropertyModel;
		this.genericPropertyTypeModel = genericPropertyTypeModel;
	}

	@Override
	public String name() {
		return rawPropertyModel.name();
	}

	@Override
	public Stream<Annotation> annotations() {
		return rawPropertyModel.annotations();
	}

	@Override
	public PojoTypeModel<T> typeModel() {
		return genericPropertyTypeModel;
	}

	@Override
	@SuppressWarnings("unchecked") // We know that, in the current generic context, this cast is legal
	public ValueReadHandle<T> handle() {
		return (ValueReadHandle<T>) rawPropertyModel.handle();
	}
}
