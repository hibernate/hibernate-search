/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchResultDefinitionContext;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.dsl.SearchResultDefinitionContext;

public interface HibernateOrmSearchManager extends PojoSearchManager {

	@Override
	default SearchResultDefinitionContext<PojoReference, Object> search() {
		return search( Collections.singleton( Object.class ) );
	}

	@Override
	default <T> SearchResultDefinitionContext<PojoReference, T> search(Class<T> type) {
		return search( Collections.singleton( type ) );
	}

	@Override
	<T> SearchResultDefinitionContext<PojoReference, T> search(Collection<? extends Class<? extends T>> types);

	default HibernateOrmSearchResultDefinitionContext<Object> searchAsFullTextQuery() {
		return searchAsFullTextQuery( Collections.singleton( Object.class ) );
	}

	default <T> HibernateOrmSearchResultDefinitionContext<T> searchAsFullTextQuery(Class<T> type) {
		return searchAsFullTextQuery( Collections.singleton( type ) );
	}

	<T> HibernateOrmSearchResultDefinitionContext<T> searchAsFullTextQuery(Collection<? extends Class<? extends T>> types);

}
