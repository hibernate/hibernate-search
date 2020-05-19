/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.context;

import org.hibernate.Session;

public interface HibernateOrmSessionContext {

	/**
	 * @return The Hibernate ORM {@link Session}.
	 */
	Session session();

	/**
	 * @return The Hibernate ORM {@link Session}.
	 * @deprecated Use {@link #session()} instead.
	 */
	@Deprecated
	default Session getSession() {
		return session();
	}

}
