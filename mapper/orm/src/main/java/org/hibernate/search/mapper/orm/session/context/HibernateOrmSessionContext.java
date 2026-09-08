/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.context;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;

public interface HibernateOrmSessionContext {

	/**
	 * @return The underlying Hibernate ORM session as a {@link SharedSessionContract},
	 * which is the common contract for both {@link Session} and
	 * {@link org.hibernate.StatelessSession StatelessSession}.
	 */
	SharedSessionContract sessionContract();

	/**
	 * @return The Hibernate ORM {@link Session}.
	 * @throws org.hibernate.search.util.common.SearchException if the underlying session is not a {@link Session}.
	 * @deprecated Use {@link #sessionContract()} instead, and unwrap to the specific session type if needed.
	 */
	@Deprecated(since = "9.0", forRemoval = true)
	Session session();

}
