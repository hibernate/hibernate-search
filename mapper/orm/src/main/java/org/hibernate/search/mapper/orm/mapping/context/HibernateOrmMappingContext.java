/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.context;

import org.hibernate.SessionFactory;

public interface HibernateOrmMappingContext {

	/**
	 * @return The Hibernate ORM {@link SessionFactory}.
	 */
	SessionFactory sessionFactory();

	/**
	 * @return The Hibernate ORM {@link SessionFactory}.
	 * @deprecated Use {@link #sessionFactory()} instead.
	 */
	@Deprecated
	default SessionFactory getSessionFactory() {
		return sessionFactory();
	}

}
