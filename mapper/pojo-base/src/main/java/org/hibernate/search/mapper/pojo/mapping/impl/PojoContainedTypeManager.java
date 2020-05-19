/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeContainedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.CachingCastingEntitySupplier;
import org.hibernate.search.mapper.pojo.work.impl.PojoContainedTypeIndexingPlan;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkContainedTypeContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * @param <E> The contained entity type.
 */
public class PojoContainedTypeManager<E>
		implements AutoCloseable, ToStringTreeAppendable,
		PojoWorkContainedTypeContext<E>, PojoScopeContainedTypeContext<E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final PojoCaster<E> caster;
	private final PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver;

	public PojoContainedTypeManager(PojoRawTypeIdentifier<E> typeIdentifier,
			PojoCaster<E> caster,
			PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver) {
		this.typeIdentifier = typeIdentifier;
		this.caster = caster;
		this.reindexingResolver = reindexingResolver;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[javaType = " + typeIdentifier + "]";
	}

	@Override
	public void close() {
		reindexingResolver.close();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "typeIdentifier", typeIdentifier )
				.attribute( "reindexingResolver", reindexingResolver );
	}

	@Override
	public PojoRawTypeIdentifier<E> getTypeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public Supplier<E> toEntitySupplier(PojoWorkSessionContext<?> sessionContext, Object entity) {
		PojoRuntimeIntrospector introspector = sessionContext.runtimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, introspector, entity );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoWorkSessionContext<?> sessionContext,
			Supplier<E> entitySupplier, Set<String> dirtyPaths) {
		PojoRuntimeIntrospector introspector = sessionContext.runtimeIntrospector();
		reindexingResolver.resolveEntitiesToReindex(
				collector, introspector, entitySupplier.get(), dirtyPaths
		);
	}

	@Override
	public PojoContainedTypeIndexingPlan<E> createIndexingPlan(PojoWorkSessionContext<?> sessionContext) {
		return new PojoContainedTypeIndexingPlan<>(
				this, sessionContext
		);
	}

}
