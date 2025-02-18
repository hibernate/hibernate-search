/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.models.spi;

import java.lang.reflect.Member;
import java.util.List;

import org.hibernate.models.spi.MemberDetails;
import org.hibernate.search.mapper.pojo.model.spi.PojoInjectablePropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadWriteHandle;

final class PojoSimpleModelsInjectablePropertyModel<T>
		extends
		AbstractPojoModelsPropertyModel<T, AbstractPojoModelsBootstrapIntrospector, ValueReadWriteHandle<T>>
		implements PojoInjectablePropertyModel<T> {

	PojoSimpleModelsInjectablePropertyModel(AbstractPojoModelsBootstrapIntrospector introspector,
			PojoSimpleModelsRawInjectableBinderModel<?> holderTypeModel,
			String name, List<MemberDetails> declaredProperties,
			List<Member> members) {
		super( introspector, holderTypeModel, name, declaredProperties, members );
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	protected ValueReadWriteHandle<T> createHandle(Member member) throws IllegalAccessException {
		return (ValueReadWriteHandle<T>) introspector.createValueReadWriteHandle( member );
	}
}
