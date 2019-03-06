/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.impl.LazyInitSearchSession;

public final class Search {

	private Search() {
		// Private constructor
	}

	/**
	 * Retrieve the {@link SearchSession} from a Hibernate ORM {@link Session}.
	 * <p>
	 * The resulting instance depends on the passed {@link Session}:
	 * closing the {@link Session} will close the {@link SearchSession}.
	 * The {@link SearchSession} will share the {@link Session}'s persistence context.
	 *
	 * @param session A Hibernate ORM session.
	 * @return The corresponding {@link SearchSession}.
	 */
	public static SearchSession getSearchSession(Session session) {
		return createSearchSession( session.unwrap( SessionImplementor.class ) );
	}

	/**
	 * Retrieve the {@link SearchSession} from a JPA {@link EntityManager}.
	 * <p>
	 * The resulting instance depends on the passed {@link EntityManager}:
	 * closing the {@link EntityManager} will close the {@link SearchSession}.
	 * The {@link SearchSession} will share the {@link EntityManager}'s persistence context.
	 *
	 * @param entityManager A JPA entity manager.
	 * @return The corresponding {@link SearchSession}.
	 */
	public static SearchSession getSearchSession(EntityManager entityManager) {
		return createSearchSession( entityManager.unwrap( SessionImplementor.class ) );
	}

	private static SearchSession createSearchSession(SessionImplementor sessionImplementor) {
		return new LazyInitSearchSession( sessionImplementor );
	}

}
