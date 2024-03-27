/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

class HibernateOrmBasicDynamicMapPropertyMetadata {
	private final HibernateOrmTypeModelFactory<?> typeModelFactory;

	HibernateOrmBasicDynamicMapPropertyMetadata(HibernateOrmTypeModelFactory<?> typeModelFactory) {
		this.typeModelFactory = typeModelFactory;
	}

	/**
	 * @return A factory of generic type models
	 */
	HibernateOrmTypeModelFactory<?> getTypeModelFactory() {
		return typeModelFactory;
	}

}
