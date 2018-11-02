/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import java.util.Collection;

import org.hibernate.search.mapper.orm.search.HibernateOrmSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.PojoWorkPlan;

public interface HibernateOrmSearchManager extends AutoCloseable {

	@Override
	void close();

	<T> HibernateOrmSearchTarget<T> search(Class<T> targetedType);

	<T> HibernateOrmSearchTarget<T> search(Collection<? extends Class<? extends T>> targetedTypes);

	/**
	 * @return A new work plan for this manager, maintaining its state (list of works) independently from the manager.
	 * Calling {@link PojoWorkPlan#execute()} is required to actually execute works,
	 * the manager will <strong>not</strong> do it automatically upon closing.
	 */
	PojoWorkPlan createWorkPlan();

}
