/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.models.spi;

import java.lang.reflect.Member;
import java.util.List;

import org.hibernate.accessor.ValueReader;
import org.hibernate.models.spi.MemberDetails;

final class PojoSimpleModelsPropertyModel<T>
		extends
		AbstractPojoModelsPropertyModel<T, AbstractPojoModelsBootstrapIntrospector> {

	PojoSimpleModelsPropertyModel(AbstractPojoModelsBootstrapIntrospector introspector,
			PojoSimpleModelsRawTypeModel<?> holderTypeModel,
			String name, List<MemberDetails> declaredProperties,
			List<Member> members) {
		super( introspector, holderTypeModel, name, declaredProperties, members );
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	protected ValueReader<T> createHandle(Member member) throws IllegalAccessException {
		return (ValueReader<T>) introspector.createValueReadHandle( member );
	}
}
