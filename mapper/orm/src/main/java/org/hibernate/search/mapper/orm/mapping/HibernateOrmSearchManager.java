/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchQueryQueryResultDefinitionContext;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchTarget;

public interface HibernateOrmSearchManager extends PojoSearchManager {

	@Override
	default PojoSearchTarget<Object> search() {
		return search( Collections.singleton( Object.class ) );
	}

	@Override
	default <T> PojoSearchTarget<T> search(Class<T> targetedType) {
		return search( Collections.singleton( targetedType ) );
	}

	@Override
	<T> PojoSearchTarget<T> search(Collection<? extends Class<? extends T>> targetedTypes);

	default HibernateOrmSearchQueryQueryResultDefinitionContext<Object> searchAsFullTextQuery() {
		return searchAsFullTextQuery( Collections.singleton( Object.class ) );
	}

	default <T> HibernateOrmSearchQueryQueryResultDefinitionContext<T> searchAsFullTextQuery(Class<T> targetedType) {
		return searchAsFullTextQuery( Collections.singleton( targetedType ) );
	}

	<T> HibernateOrmSearchQueryQueryResultDefinitionContext<T> searchAsFullTextQuery(Collection<? extends Class<? extends T>> targetedTypes);

}
