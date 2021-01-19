/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <E> The contained entity type.
 */
public interface PojoWorkTypeContext<E> {

	PojoRawTypeIdentifier<E> typeIdentifier();

	Supplier<E> toEntitySupplier(PojoWorkSessionContext<?> sessionContext, Object entity);

	PojoPathFilter dirtySelfOrContainingFilter();

	void resolveEntitiesToReindex(PojoReindexingCollector collector,
			PojoWorkSessionContext<?> sessionContext, Object identifier,
			Supplier<E> entitySupplier,
			PojoImplicitReindexingResolverRootContext context);

}
