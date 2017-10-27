/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.hibernate;

import java.util.Collection;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.jpa.FullTextEntityManager;

public interface FullTextSession extends Session, FullTextEntityManager {

	@Override
	HibernateOrmSearchResultDefinitionContext<Object> search();

	@Override
	<T> HibernateOrmSearchResultDefinitionContext<T> search(Class<T> type);

	@Override
	<T> HibernateOrmSearchResultDefinitionContext<T> search(Collection<? extends Class<? extends T>> types);

}
