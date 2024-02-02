/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverRootContext;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.identity.impl.IdentityMappingMode;
import org.hibernate.search.mapper.pojo.identity.impl.PojoRootIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.loading.definition.spi.PojoEntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinals;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.work.impl.CachingCastingEntitySupplier;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkTypeContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * @param <I> The identifier type for the entity type.
 * @param <E> The entity type.
 */
public abstract class AbstractPojoTypeManager<I, E>
		implements AutoCloseable, ToStringTreeAppendable, PojoWorkTypeContext<I, E> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final PojoRawTypeIdentifier<E> typeIdentifier;
	private final List<PojoRawTypeIdentifier<? super E>> ascendingSuperTypes;
	protected final PojoCaster<E> caster;
	protected final String entityName;
	protected final String secondaryEntityName;
	private final boolean singleConcreteTypeInEntityHierarchy;
	protected final IdentifierMappingImplementor<I, E> identifierMapping;
	private final PojoPathOrdinals pathOrdinals;
	protected final PojoImplicitReindexingResolver<E> reindexingResolver;
	private final Optional<PojoSelectionLoadingStrategy<? super E>> selectionLoadingStrategyOptional;
	private final Optional<PojoMassLoadingStrategy<? super E, ?>> massLoadingStrategyOptional;

	public AbstractPojoTypeManager(Builder<E> builder, IdentifierMappingImplementor<I, E> identifierMapping) {
		this.typeIdentifier = builder.typeModel.typeIdentifier();
		this.ascendingSuperTypes = builder.typeModel.ascendingSuperTypes()
				.map( PojoRawTypeModel::typeIdentifier )
				.collect( Collectors.toUnmodifiableList() );
		this.caster = builder.typeModel.caster();
		this.entityName = builder.entityName;
		this.secondaryEntityName = builder.secondaryEntityName;
		this.singleConcreteTypeInEntityHierarchy = builder.singleConcreteTypeInEntityHierarchy;
		this.identifierMapping = identifierMapping;
		this.pathOrdinals = builder.pathOrdinals;
		this.reindexingResolver = builder.reindexingResolver;
		this.selectionLoadingStrategyOptional = Optional.ofNullable( builder.selectionLoadingStrategy );
		this.massLoadingStrategyOptional = Optional.ofNullable( builder.massLoadingStrategy );
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
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "entityName", entityName )
				.attribute( "typeIdentifier", typeIdentifier )
				.attribute( "identifierMapping", identifierMapping )
				.attribute( "reindexingResolver", reindexingResolver )
				.attribute( "selectionLoadingStrategy", selectionLoadingStrategyOptional.orElse( null ) )
				.attribute( "massLoadingStrategy", massLoadingStrategyOptional.orElse( null ) );
	}

	@Override
	public final PojoRawTypeIdentifier<E> typeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public final List<PojoRawTypeIdentifier<? super E>> ascendingSuperTypes() {
		return ascendingSuperTypes;
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
	public String secondaryEntityName() {
		return secondaryEntityName;
	}

	public String name() {
		return entityName;
	}

	public Class<?> javaClass() {
		return typeIdentifier.javaClass();
	}

	public boolean loadingAvailable() {
		return selectionLoadingStrategyOptional.isPresent();
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

	@Override
	public PojoSelectionLoadingStrategy<? super E> selectionLoadingStrategy() {
		return selectionLoadingStrategyOptional()
				.orElseThrow( () -> log.noSelectionLoadingStrategy( entityName ) );
	}

	@Override
	public Optional<PojoSelectionLoadingStrategy<? super E>> selectionLoadingStrategyOptional() {
		return selectionLoadingStrategyOptional;
	}

	@Override
	public PojoMassLoadingStrategy<? super E, ?> massLoadingStrategy() {
		return massLoadingStrategyOptional()
				.orElseThrow( () -> log.noMassLoadingStrategy( entityName ) );
	}

	@Override
	public Optional<PojoMassLoadingStrategy<? super E, ?>> massLoadingStrategyOptional() {
		return massLoadingStrategyOptional;
	}

	public abstract static class Builder<E> {
		public final PojoRawTypeModel<E> typeModel;
		private final String entityName;
		private final String secondaryEntityName;

		private PojoRootIdentityMappingCollector<E> identityMappingCollector;
		protected IdentifierMappingImplementor<?, E> identifierMapping;

		private PojoImplicitReindexingResolver<E> reindexingResolver;

		private Boolean singleConcreteTypeInEntityHierarchy;
		private PojoPathOrdinals pathOrdinals;
		private PojoSelectionLoadingStrategy<? super E> selectionLoadingStrategy;
		private PojoMassLoadingStrategy<? super E, ?> massLoadingStrategy;

		protected boolean closed = false;

		Builder(PojoRawTypeModel<E> typeModel, String entityName, String secondaryEntityName,
				PojoRootIdentityMappingCollector<E> identityMappingCollector) {
			this.typeModel = typeModel;
			this.entityName = entityName;
			this.secondaryEntityName = secondaryEntityName;
			this.identityMappingCollector = identityMappingCollector;
		}

		public final void closeOnFailure() {
			if ( closed ) {
				return;
			}
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				doCloseOnFailure( closer );
				closed = true;
			}
		}

		protected void doCloseOnFailure(Closer<RuntimeException> closer) {
			closer.push( PojoRootIdentityMappingCollector::closeOnFailure, identityMappingCollector );
			closer.push( IdentifierMappingImplementor::close, identifierMapping );
			closer.push( PojoImplicitReindexingResolver::close, reindexingResolver );
		}

		protected abstract PojoTypeExtendedMappingCollector extendedMappingCollector();

		public void preBuildIdentifierMapping(IdentityMappingMode identityMappingMode) {
			if ( this.identifierMapping != null ) {
				throw new AssertionFailure( "Internal error - preBuildIdentifierMapping should be called only once" );
			}
			this.identifierMapping = this.identityMappingCollector.buildAndContributeTo(
					extendedMappingCollector(), identityMappingMode );
			this.identityMappingCollector = null;
		}

		public void reindexingResolver(PojoImplicitReindexingResolver<E> reindexingResolver) {
			if ( this.reindexingResolver != null ) {
				throw new AssertionFailure( "Internal error - reindexingResolver should be called only once" );
			}
			this.reindexingResolver = reindexingResolver;
			extendedMappingCollector().dirtyFilter( reindexingResolver.dirtySelfOrContainingFilter() );
			extendedMappingCollector().dirtyContainingAssociationFilter(
					reindexingResolver.associationInverseSideResolver().dirtyContainingAssociationFilter() );
		}

		public void preBuildOtherMetadata(boolean singleConcreteTypeInEntityHierarchy,
				PojoPathOrdinals pathOrdinals, Optional<?> loadingBinderOptional) {
			this.singleConcreteTypeInEntityHierarchy = singleConcreteTypeInEntityHierarchy;
			this.pathOrdinals = pathOrdinals;
			preBuildLoadingConfiguration( loadingBinderOptional );
		}

		private void preBuildLoadingConfiguration(Optional<?> loadingBinderOptional) {
			if ( loadingBinderOptional.isEmpty() ) {
				return;
			}
			extendedMappingCollector().applyLoadingBinder( loadingBinderOptional.get(),
					new PojoEntityLoadingBindingContext() {
						@Override
						@SuppressWarnings("unchecked") // Checked using reflection
						public <E2> void selectionLoadingStrategy(Class<E2> expectedEntitySuperType,
								PojoSelectionLoadingStrategy<? super E2> strategy) {
							checkEntitySuperType( expectedEntitySuperType );
							selectionLoadingStrategy = (PojoSelectionLoadingStrategy<? super E>) strategy;
						}

						@Override
						@SuppressWarnings("unchecked") // Checked using reflection
						public <E2> void massLoadingStrategy(Class<E2> expectedEntitySuperType,
								PojoMassLoadingStrategy<? super E2, ?> strategy) {
							checkEntitySuperType( expectedEntitySuperType );
							massLoadingStrategy = (PojoMassLoadingStrategy<? super E, ?>) strategy;
						}

						private <E2> void checkEntitySuperType(Class<E2> expectedEntitySuperType) {
							if ( !expectedEntitySuperType.isAssignableFrom( typeModel.typeIdentifier().javaClass() ) ) {
								throw log.loadingConfigurationTypeMismatch( typeModel, expectedEntitySuperType );
							}
						}
					} );
		}

		public abstract AbstractPojoTypeManager<?, E> build();

	}
}
