/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm;

import java.lang.invoke.MethodHandles;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.search.query.impl.HibernateOrmSearchQueryAdapter;
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

	/**
	 * Convert a {@link SearchQuery} to a {@link TypedQuery JPA query}.
	 * <p>
	 * Note that the resulting query <strong>does not support all operations</strong>
	 * and may behave slightly differently from what is expected from a {@link TypedQuery} in some cases
	 * (including, but not limited to, the type of thrown exceptions).
	 * For these reasons, it is recommended to only use this method when absolutely required,
	 * for example when integrating to an external library that expects JPA queries.
	 *
	 * @param searchQuery The search query to convert.
	 * @param <T> The type of query hits.
	 * @return A representation of the given query as a JPA query.
	 */
	public static <T> TypedQuery<T> toJpaQuery(SearchQuery<T> searchQuery) {
		return HibernateOrmSearchQueryAdapter.create( searchQuery );
	}

	/**
	 * Convert a {@link SearchQuery} to a {@link Query Hibernate ORM query}.
	 * <p>
	 * Note that the resulting query <strong>does not support all operations</strong>
	 * and may behave slightly differently from what is expected from a {@link Query} in some cases
	 * (including, but not limited to, the type of thrown exceptions).
	 * For these reasons, it is recommended to only use this method when absolutely required,
	 * for example when integrating to an external library that expects Hibernate ORM queries.
	 *
	 * @param searchQuery The search query to convert.
	 * @param <T> The type of query hits.
	 * @return A representation of the given query as a Hibernate ORM query.
	 */
	public static <T> Query<T> toOrmQuery(SearchQuery<T> searchQuery) {
		return HibernateOrmSearchQueryAdapter.create( searchQuery );
	}

	private static SearchSession createSearchSession(SessionImplementor sessionImplementor) {
		return new LazyInitSearchSession( sessionImplementor );
	}

}
