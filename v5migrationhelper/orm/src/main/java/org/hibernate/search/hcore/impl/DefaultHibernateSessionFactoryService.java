/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import org.hibernate.SessionFactory;

/**
 * A Hibernate Search service which allows to request a Hibernate {@code SessionFactory} during bootstrapping.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class DefaultHibernateSessionFactoryService implements HibernateSessionFactoryService {

	private final SessionFactory sessionFactory;

	DefaultHibernateSessionFactoryService(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

}
