/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.context;

import org.hibernate.Session;

public interface HibernateOrmSessionContext {

	/**
	 * @return The Hibernate ORM {@link Session}.
	 */
	Session session();

}
