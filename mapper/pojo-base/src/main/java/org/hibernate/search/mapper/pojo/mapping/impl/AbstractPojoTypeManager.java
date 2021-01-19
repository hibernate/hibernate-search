/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.impl.CachingCastingEntitySupplier;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <E> The entity type.
 */
public class AbstractPojoTypeManager<E>
		implements AutoCloseable, ToStringTreeAppendable,
				PojoWorkTypeContext<E> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String entityName;
	protected final PojoRawTypeIdentifier<E> typeIdentifier;
	protected final PojoCaster<E> caster;
	protected final PojoImplicitReindexingResolver<E> reindexingResolver;

	public AbstractPojoTypeManager(String entityName, PojoRawTypeIdentifier<E> typeIdentifier,
			PojoCaster<E> caster,
			PojoImplicitReindexingResolver<E> reindexingResolver) {
		this.entityName = entityName;
		this.typeIdentifier = typeIdentifier;
		this.caster = caster;
		this.reindexingResolver = reindexingResolver;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[entityName = " + entityName + ", javaType = " + typeIdentifier + "]";
	}

	@Override
	public void close() {
		reindexingResolver.close();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "entityName", entityName )
				.attribute( "typeIdentifier", typeIdentifier )
				.attribute( "reindexingResolver", reindexingResolver );
	}

	@Override
	public final PojoRawTypeIdentifier<E> typeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public final Supplier<E> toEntitySupplier(PojoWorkSessionContext<?> sessionContext, Object entity) {
		PojoRuntimeIntrospector introspector = sessionContext.runtimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, introspector, entity );
	}

	@Override
	public PojoPathFilter dirtySelfOrContainingFilter() {
		return reindexingResolver.dirtySelfOrContainingFilter();
	}

	@Override
	public final void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoWorkSessionContext<?> sessionContext,
			Object identifier, Supplier<E> entitySupplier,
			PojoImplicitReindexingResolverRootContext context) {
		try {
			reindexingResolver.resolveEntitiesToReindex( collector, entitySupplier.get(), context );
		}
		catch (RuntimeException e) {
			Object entityReference = EntityReferenceFactory.safeCreateEntityReference(
					sessionContext.entityReferenceFactory(), entityName, identifier, e::addSuppressed );
			throw log.errorResolvingEntitiesToReindex( entityReference, e.getMessage(), e );
		}
	}

}
