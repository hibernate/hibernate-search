/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.search.dsl.query.SearchQueryResultDefinitionContext;

public interface SearchSession {

	default <T> SearchQueryResultDefinitionContext<T> search(Class<T> type) {
		return scope( type ).search();
	}

	default <T> SearchQueryResultDefinitionContext<T> search(Collection<? extends Class<? extends T>> types) {
		return scope( types ).search();
	}

	default <T> SearchScope<T> scope(Class<T> type) {
		return scope( Collections.singleton( type ) );
	}

	<T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types);

	MassIndexer createIndexer(Class<?>... types);

	EntityManager toJpaEntityManager();

	Session toHibernateOrmSession();

}
