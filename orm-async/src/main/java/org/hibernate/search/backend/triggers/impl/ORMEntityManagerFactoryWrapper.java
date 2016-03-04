/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.triggers.impl;

import org.hibernate.SessionFactory;
import org.hibernate.search.db.util.impl.EntityManagerFactoryWrapper;
import org.hibernate.search.db.util.impl.EntityManagerWrapper;

/**
 * Created by Martin on 12.11.2015.
 */
public final class ORMEntityManagerFactoryWrapper implements EntityManagerFactoryWrapper {

	private final SessionFactory sessionFactory;

	public ORMEntityManagerFactoryWrapper(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public EntityManagerWrapper createEntityManager() {
		return new ORMEntityManagerWrapper( this.sessionFactory.openSession() );
	}

	@Override
	public boolean isOpen() {
		return !this.sessionFactory.isClosed();
	}

}
