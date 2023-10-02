/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.context;

import org.hibernate.SessionFactory;

public interface HibernateOrmMappingContext {

	/**
	 * @return The Hibernate ORM {@link SessionFactory}.
	 */
	SessionFactory sessionFactory();

}
