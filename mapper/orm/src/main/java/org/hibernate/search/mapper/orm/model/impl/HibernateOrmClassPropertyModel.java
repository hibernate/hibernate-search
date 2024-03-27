/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.reflect.Member;
import java.util.List;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnPropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

class HibernateOrmClassPropertyModel<T>
		extends AbstractPojoHCAnnPropertyModel<T, HibernateOrmBootstrapIntrospector> {

	private final HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata;

	HibernateOrmClassPropertyModel(HibernateOrmBootstrapIntrospector introspector,
			HibernateOrmClassRawTypeModel<?> holderTypeModel,
			String name, List<XProperty> declaredXProperties,
			HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata,
			List<Member> members) {
		super( introspector, holderTypeModel, name, declaredXProperties, members );
		this.ormPropertyMetadata = ormPropertyMetadata;
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	protected ValueReadHandle<T> createHandle(Member member) throws IllegalAccessException {
		return (ValueReadHandle<T>) introspector.createValueReadHandle( holderTypeModel.typeIdentifier().javaClass(),
				member, ormPropertyMetadata );
	}

}
