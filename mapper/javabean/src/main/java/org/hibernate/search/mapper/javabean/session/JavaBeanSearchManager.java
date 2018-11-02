/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.mapper.javabean.search.JavaBeanSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.PojoWorkPlan;

public interface JavaBeanSearchManager extends AutoCloseable {

	@Override
	void close();

	default <T> JavaBeanSearchTarget search(Class<T> targetedType) {
		return search( Collections.singleton( targetedType ) );
	}

	<T> JavaBeanSearchTarget search(Collection<? extends Class<? extends T>> targetedTypes);

	/**
	 * @return The main work plan for this manager. Calling {@link PojoWorkPlan#execute()}
	 * is optional, as it will be executed upon closing this manager.
	 */
	PojoWorkPlan getMainWorkPlan();

}
