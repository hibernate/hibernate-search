/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.reflect.Member;
import java.util.List;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

final class PojoSimpleHCAnnPropertyModel<T> extends AbstractPojoHCAnnPropertyModel<T, AbstractPojoHCAnnBootstrapIntrospector> {

	PojoSimpleHCAnnPropertyModel(AbstractPojoHCAnnBootstrapIntrospector introspector,
			PojoSimpleHCAnnRawTypeModel<?> holderTypeModel,
			String name, List<XProperty> declaredXProperties,
			List<Member> members) {
		super( introspector, holderTypeModel, name, declaredXProperties, members );
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	protected ValueReadHandle<T> createHandle(Member member) throws IllegalAccessException {
		return (ValueReadHandle<T>) introspector.createValueReadHandle( member );
	}
}
