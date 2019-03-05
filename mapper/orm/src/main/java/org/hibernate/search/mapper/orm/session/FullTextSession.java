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
import org.hibernate.search.mapper.orm.search.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.search.dsl.query.FullTextQueryResultDefinitionContext;

public interface FullTextSession {

	default <T> FullTextQueryResultDefinitionContext<T> search(Class<T> type) {
		return target( type ).search();
	}

	default <T> FullTextQueryResultDefinitionContext<T> search(Collection<? extends Class<? extends T>> types) {
		return target( types ).search();
	}

	default <T> FullTextSearchTarget<T> target(Class<T> type) {
		return target( Collections.singleton( type ) );
	}

	<T> FullTextSearchTarget<T> target(Collection<? extends Class<? extends T>> types);

	MassIndexer createIndexer(Class<?>... types);

	EntityManager toJpaEntityManager();

	Session toHibernateOrmSession();

}
