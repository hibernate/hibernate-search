/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeContainedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoContainedTypeIndexingPlan;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingPlanImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkContainedTypeContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <E> The contained entity type.
 */
public class PojoContainedTypeManager<E> extends AbstractPojoTypeManager<E>
		implements PojoWorkContainedTypeContext<E>, PojoScopeContainedTypeContext<E> {
	public PojoContainedTypeManager(String entityName, PojoRawTypeIdentifier<E> typeIdentifier,
			PojoCaster<E> caster,
			PojoImplicitReindexingResolver<E> reindexingResolver) {
		super( entityName, typeIdentifier, caster, reindexingResolver );
	}

	@Override
	public PojoContainedTypeIndexingPlan<E> createIndexingPlan(PojoWorkSessionContext<?> sessionContext,
			PojoIndexingPlanImpl<?> root) {
		return new PojoContainedTypeIndexingPlan<>( this, sessionContext, root );
	}

}
