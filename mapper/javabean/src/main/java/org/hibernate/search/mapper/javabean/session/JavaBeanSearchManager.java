/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.mapper.javabean.search.JavaBeanSearchScope;
import org.hibernate.search.mapper.javabean.search.dsl.query.JavaBeanQueryResultDefinitionContext;
import org.hibernate.search.mapper.javabean.work.JavaBeanWorkPlan;

public interface JavaBeanSearchManager extends AutoCloseable {

	@Override
	void close();

	default <T> JavaBeanQueryResultDefinitionContext search(Class<T> type) {
		return scope( type ).search();
	}

	default <T> JavaBeanQueryResultDefinitionContext search(Collection<? extends Class<? extends T>> types) {
		return scope( types ).search();
	}

	default <T> JavaBeanSearchScope scope(Class<T> type) {
		return scope( Collections.singleton( type ) );
	}

	<T> JavaBeanSearchScope scope(Collection<? extends Class<? extends T>> types);

	/**
	 * @return The main work plan for this manager. It will be executed upon closing this manager.
	 */
	JavaBeanWorkPlan getMainWorkPlan();

}
