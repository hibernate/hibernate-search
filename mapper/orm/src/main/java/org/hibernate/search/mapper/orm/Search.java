/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm;

import java.lang.invoke.MethodHandles;
import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.impl.LazyInitSearchSession;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class Search {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
	 * @throws org.hibernate.search.util.common.SearchException if the session NOT {@link Session#isOpen()}.
	 */
	public static SearchSession getSearchSession(Session session) {
		SessionImplementor sessionImpl = null;
		try {
			sessionImpl = session.unwrap( SessionImplementor.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionAccessError( e );
		}

		return createSearchSession( sessionImpl );
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
	 * @throws org.hibernate.search.util.common.SearchException if the entity manager NOT {@link EntityManager#isOpen()}.
	 */
	public static SearchSession getSearchSession(EntityManager entityManager) {
		SessionImplementor sessionImpl = null;
		try {
			sessionImpl = entityManager.unwrap( SessionImplementor.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionAccessError( e );
		}

		return createSearchSession( sessionImpl );
	}

	private static SearchSession createSearchSession(SessionImplementor sessionImplementor) {
		return new LazyInitSearchSession( sessionImplementor );
	}

}
