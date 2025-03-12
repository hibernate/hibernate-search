/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.util.Collection;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface PojoInjectableBinderModel<T> extends PojoRawTypeModel<T> {

	@Override
	PojoInjectablePropertyModel<?> property(String propertyName);

	@Override
	Collection<PojoInjectablePropertyModel<?>> declaredProperties();

}
