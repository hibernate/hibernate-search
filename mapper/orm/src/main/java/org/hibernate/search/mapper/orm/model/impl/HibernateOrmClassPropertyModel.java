/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.reflect.Member;
import java.util.List;

import org.hibernate.models.spi.MemberDetails;
import org.hibernate.search.mapper.pojo.model.models.spi.AbstractPojoModelsPropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

class HibernateOrmClassPropertyModel<T>
		extends AbstractPojoModelsPropertyModel<T, HibernateOrmBootstrapIntrospector, ValueReadHandle<T>> {

	private final HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata;

	HibernateOrmClassPropertyModel(HibernateOrmBootstrapIntrospector introspector,
			HibernateOrmClassRawTypeModel<?> holderTypeModel,
			String name, List<MemberDetails> declaredProperties,
			HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata,
			List<Member> members) {
		super( introspector, holderTypeModel, name, declaredProperties, members );
		this.ormPropertyMetadata = ormPropertyMetadata;
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	protected ValueReadHandle<T> createHandle(Member member) throws IllegalAccessException {
		return (ValueReadHandle<T>) introspector.createValueReadHandle( holderTypeModel.typeIdentifier().javaClass(),
				member, ormPropertyMetadata );
	}

}
