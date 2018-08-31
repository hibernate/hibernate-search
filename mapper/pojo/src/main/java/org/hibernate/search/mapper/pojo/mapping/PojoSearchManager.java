/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.common.SearchManager;


/**
 * @author Yoann Rodiere
 */
public interface PojoSearchManager extends SearchManager {

	default PojoSearchTarget<?> search() {
		return search( Collections.singleton( Object.class ) );
	}

	default <T> PojoSearchTarget<?> search(Class<T> targetedType) {
		return search( Collections.singleton( targetedType ) );
	}

	<T> PojoSearchTarget<?> search(Collection<? extends Class<? extends T>> targetedTypes);

	/**
	 * @return The main work plan for this manager. Calling {@link PojoWorkPlan#execute()}
	 * is optional, as it will be executed upon closing this manager.
	 */
	PojoWorkPlan getMainWorkPlan();

	/**
	 * @return A new work plan for this manager, maintaining its state (list of works) independently from the manager.
	 * Calling {@link PojoWorkPlan#execute()} is required to actually execute works,
	 * the manager will <strong>not</strong> do it automatically upon closing.
	 */
	PojoWorkPlan createWorkPlan();

}
