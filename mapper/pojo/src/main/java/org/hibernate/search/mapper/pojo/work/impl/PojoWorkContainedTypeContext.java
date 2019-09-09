/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;

/**
 * @param <E> The contained entity type.
 */
public interface PojoWorkContainedTypeContext<E> {

	Class<E> getJavaClass();

	Supplier<E> toEntitySupplier(AbstractPojoBackendSessionContext sessionContext, Object entity);

	void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoRuntimeIntrospector runtimeIntrospector,
			Supplier<E> entitySupplier, Set<String> dirtyPaths);

	PojoContainedTypeWorkPlan<E> createWorkPlan(AbstractPojoBackendSessionContext sessionContext);

}
