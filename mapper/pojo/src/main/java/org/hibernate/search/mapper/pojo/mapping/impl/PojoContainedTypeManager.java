/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.impl.CachingCastingEntitySupplier;
import org.hibernate.search.mapper.pojo.work.impl.PojoContainedTypeWorkPlan;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * @param <E> The contained entity type.
 */
public class PojoContainedTypeManager<E> implements AutoCloseable, ToStringTreeAppendable {

	private final Class<E> javaClass;
	private final PojoCaster<E> caster;
	private final PojoMappingTypeMetadata mappingMetadata;
	private final PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver;

	public PojoContainedTypeManager(Class<E> javaClass,
			PojoCaster<E> caster,
			PojoMappingTypeMetadata mappingMetadata,
			PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver) {
		this.javaClass = javaClass;
		this.caster = caster;
		this.mappingMetadata = mappingMetadata;
		this.reindexingResolver = reindexingResolver;
	}

	@Override
	public void close() {
		reindexingResolver.close();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "javaClass", javaClass )
				.attribute( "reindexingResolver", reindexingResolver );
	}

	public Class<E> getJavaClass() {
		return javaClass;
	}

	PojoMappingTypeMetadata getMappingMetadata() {
		return mappingMetadata;
	}

	public Supplier<E> toEntitySupplier(AbstractPojoSessionContextImplementor sessionContext, Object entity) {
		PojoRuntimeIntrospector introspector = sessionContext.getRuntimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, introspector, entity );
	}

	public void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoRuntimeIntrospector runtimeIntrospector,
			Supplier<E> entitySupplier, Set<String> dirtyPaths) {
		reindexingResolver.resolveEntitiesToReindex(
				collector, runtimeIntrospector, entitySupplier.get(), dirtyPaths
		);
	}

	public PojoContainedTypeWorkPlan<E> createWorkPlan(AbstractPojoSessionContextImplementor sessionContext) {
		return new PojoContainedTypeWorkPlan<>(
				this, sessionContext
		);
	}
}
