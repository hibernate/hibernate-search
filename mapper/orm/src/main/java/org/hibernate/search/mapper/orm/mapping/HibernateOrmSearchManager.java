/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;

public interface HibernateOrmSearchManager extends PojoSearchManager {

	@Override
	default HibernateOrmSearchTarget<Object> search() {
		return search( Collections.singleton( Object.class ) );
	}

	@Override
	default <T> HibernateOrmSearchTarget<T> search(Class<T> targetedType) {
		return search( Collections.singleton( targetedType ) );
	}

	@Override
	<T> HibernateOrmSearchTarget<T> search(Collection<? extends Class<? extends T>> targetedTypes);

}
