/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * @param <E> The entity type.
 */
public interface HibernateOrmSessionTypeContext<E> {

	PojoRawTypeIdentifier<E> typeIdentifier();

	String jpaEntityName();

}
