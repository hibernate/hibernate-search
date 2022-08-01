/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * A helper to abstract away all the complexity of wrapping sections of code in a session (and transaction).
 * <p>
 * Particularly useful for unit testing of code that abstracts away from Hibernate ORM sessions.
 */
public final class SessionHelper {

	private final SessionFactoryImplementor sessionFactory;
	private final String tenantId;

	public SessionHelper(SessionFactoryImplementor sessionFactory, String tenantId) {
		this.sessionFactory = sessionFactory;
		this.tenantId = tenantId;
	}

	public SessionImplementor openSession() {
		return (SessionImplementor) sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession();
	}
}
