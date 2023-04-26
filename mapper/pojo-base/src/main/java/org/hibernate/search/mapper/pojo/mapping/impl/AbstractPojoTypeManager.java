/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinals;
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
 * @param <I> The identifier type for the entity type.
 * @param <E> The entity type.
 */
public class AbstractPojoTypeManager<I, E>
		implements AutoCloseable, ToStringTreeAppendable, PojoWorkTypeContext<I, E> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String entityName;
	protected final PojoRawTypeIdentifier<E> typeIdentifier;
	protected final PojoCaster<E> caster;
	private final boolean singleConcreteTypeInEntityHierarchy;
	protected final IdentifierMappingImplementor<I, E> identifierMapping;
	private final PojoPathOrdinals pathOrdinals;
	protected final PojoImplicitReindexingResolver<E> reindexingResolver;

	public AbstractPojoTypeManager(String entityName, PojoRawTypeIdentifier<E> typeIdentifier,
			PojoCaster<E> caster, boolean singleConcreteTypeInEntityHierarchy,
			IdentifierMappingImplementor<I, E> identifierMapping, PojoPathOrdinals pathOrdinals,
			PojoImplicitReindexingResolver<E> reindexingResolver) {
		this.entityName = entityName;
		this.typeIdentifier = typeIdentifier;
		this.caster = caster;
		this.singleConcreteTypeInEntityHierarchy = singleConcreteTypeInEntityHierarchy;
		this.identifierMapping = identifierMapping;
		this.pathOrdinals = pathOrdinals;
		this.reindexingResolver = reindexingResolver;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		AbstractPojoTypeManager<?, ?> that = (AbstractPojoTypeManager<?, ?>) o;
		return typeIdentifier.equals( that.typeIdentifier );
	}

	@Override
	public int hashCode() {
		return typeIdentifier.hashCode();
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
				.attribute( "identifierMapping", identifierMapping )
				.attribute( "reindexingResolver", reindexingResolver );
	}

	@Override
	public final PojoRawTypeIdentifier<E> typeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public Optional<PojoIndexedTypeManager<I, E>> asIndexed() {
		return Optional.empty();
	}

	@Override
	public Optional<PojoContainedTypeManager<I, E>> asContained() {
		return Optional.empty();
	}

	@Override
	public String entityName() {
		return entityName;
	}

	@Override
	public final boolean isSingleConcreteTypeInEntityHierarchy() {
		return singleConcreteTypeInEntityHierarchy;
	}

	@Override
	public IdentifierMappingImplementor<I, E> identifierMapping() {
		return identifierMapping;
	}

	@Override
	public String toDocumentIdentifier(PojoWorkSessionContext sessionContext, I identifier) {
		return identifierMapping.toDocumentIdentifier( identifier, sessionContext.mappingContext() );
	}

	@Override
	public PojoPathOrdinals pathOrdinals() {
		return pathOrdinals;
	}

	@Override
	public PojoImplicitReindexingResolver<E> reindexingResolver() {
		return reindexingResolver;
	}

	@Override
	public E toEntity(Object unproxiedEntity) {
		return caster.cast( unproxiedEntity );
	}

	@Override
	public final Supplier<E> toEntitySupplier(PojoWorkSessionContext sessionContext, Object entity) {
		if ( entity == null ) {
			return null;
		}
		PojoRuntimeIntrospector introspector = sessionContext.runtimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, introspector, entity );
	}

	@Override
	public final void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoWorkSessionContext sessionContext,
			Object identifier, Supplier<E> entitySupplier,
			PojoImplicitReindexingResolverRootContext context) {
		try {
			reindexingResolver.resolveEntitiesToReindex( collector, entitySupplier.get(), context );
		}
		catch (RuntimeException e) {
			EntityReference entityReference = sessionContext.mappingContext().entityReferenceFactoryDelegate()
					.create( typeIdentifier, entityName, identifier );
			throw log.errorResolvingEntitiesToReindex( entityReference, e.getMessage(), e );
		}
	}

}
