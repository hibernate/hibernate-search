/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.DocumentRouter;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.NoOpDocumentRouter;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.RoutingBridgeDocumentRouter;
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.identity.impl.PojoRootIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingIndexedTypeContext;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIndexingProcessorOriginalTypeNodeBuilder;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoDocumentContributor;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 */
public class PojoIndexedTypeManager<I, E> extends AbstractPojoTypeManager<I, E>
		implements PojoWorkIndexedTypeContext<I, E>, PojoScopeIndexedTypeContext<I, E>,
		PojoMassIndexingIndexedTypeContext<E>, ProjectionMappedTypeContext {
	private final DocumentRouter<? super E> documentRouter;
	private final PojoIndexingProcessor<E> processor;
	private final MappedIndexManager indexManager;

	public PojoIndexedTypeManager(Builder<E> builder, IdentifierMappingImplementor<I, E> identifierMapping) {
		super( builder, identifierMapping );
		this.documentRouter = builder.routingBridge != null
				? new RoutingBridgeDocumentRouter<>( builder.routingBridge.getBridgeHolder() )
				: NoOpDocumentRouter.INSTANCE;
		this.processor = builder.indexingProcessor;
		this.indexManager = builder.indexManager;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierMappingImplementor::close, identifierMapping );
			closer.push( DocumentRouter::close, documentRouter );
			closer.push( PojoIndexingProcessor::close, processor );
			closer.push( PojoImplicitReindexingResolver::close, reindexingResolver );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		super.appendTo( appender );
		appender.attribute( "documentRouter", documentRouter )
				.attribute( "processor", processor )
				.attribute( "indexManager", indexManager );
	}

	@Override
	public Optional<PojoIndexedTypeManager<I, E>> asIndexed() {
		return Optional.of( this );
	}

	@Override
	public DocumentRouter<? super E> router() {
		return documentRouter;
	}

	@Override
	public PojoDocumentContributor<E> toDocumentContributor(PojoWorkSessionContext sessionContext,
			PojoIndexingProcessorRootContext processorContext,
			I identifier, Supplier<E> entitySupplier) {
		return new PojoDocumentContributor<>( typeIdentifier, entityName, processor, sessionContext, processorContext,
				identifier, entitySupplier );
	}

	@Override
	public PojoPathFilter dirtySelfFilter() {
		return reindexingResolver.dirtySelfFilter();
	}

	@Override
	public IndexSchemaManager schemaManager() {
		return indexManager.schemaManager();
	}

	@Override
	public IndexIndexer createIndexer(PojoWorkSessionContext sessionContext) {
		return indexManager.createIndexer( sessionContext );
	}

	@Override
	public IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantIds) {
		return indexManager.createWorkspace( mappingContext, tenantIds );
	}

	@Override
	public IndexIndexingPlan createIndexingPlan(PojoWorkSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return indexManager.createIndexingPlan( sessionContext,
				commitStrategy, refreshStrategy );
	}

	@Override
	public <R, E2> MappedIndexScopeBuilder<R, E2> createScopeBuilder(BackendMappingContext mappingContext) {
		return indexManager.createScopeBuilder( mappingContext );
	}

	@Override
	public void addTo(MappedIndexScopeBuilder<?, ?> builder) {
		indexManager.addTo( builder );
	}

	public static class Builder<E> extends AbstractPojoTypeManager.Builder<E> {

		public final PojoIndexedTypeExtendedMappingCollector extendedMappingCollector;

		public final BoundRoutingBridge<E> routingBridge;

		private PojoIndexingProcessorOriginalTypeNodeBuilder<E> indexingProcessorBuilder;
		private PojoIndexingProcessor<E> indexingProcessor;

		private MappedIndexManagerBuilder indexManagerBuilder;
		private MappedIndexManager indexManager;

		Builder(PojoRawTypeModel<E> typeModel, String entityName, String secondaryEntityName,
				PojoRootIdentityMappingCollector<E> identityMappingCollector,
				PojoIndexedTypeExtendedMappingCollector extendedMappingCollector,
				BoundRoutingBridge<E> routingBridge,
				PojoIndexingProcessorOriginalTypeNodeBuilder<E> indexingProcessorBuilder,
				MappedIndexManagerBuilder indexManagerBuilder) {
			super( typeModel, entityName, secondaryEntityName, identityMappingCollector );
			this.extendedMappingCollector = extendedMappingCollector;
			this.routingBridge = routingBridge;
			this.indexManagerBuilder = indexManagerBuilder;
			this.indexingProcessorBuilder = indexingProcessorBuilder;
		}

		@Override
		protected void doCloseOnFailure(Closer<RuntimeException> closer) {
			super.doCloseOnFailure( closer );
			closer.push( RoutingBridge::close, routingBridge, BoundRoutingBridge::getBridge );
			closer.push( BeanHolder::close, routingBridge, BoundRoutingBridge::getBridgeHolder );
			closer.push( PojoIndexingProcessorOriginalTypeNodeBuilder::closeOnFailure, indexingProcessorBuilder );
			closer.push( PojoIndexingProcessor::close, indexingProcessor );
		}

		@Override
		protected PojoTypeExtendedMappingCollector extendedMappingCollector() {
			return extendedMappingCollector;
		}

		public void preBuildIndexingProcessor(PojoIndexingDependencyCollectorTypeNode<E> dependencyCollector) {
			if ( indexingProcessor != null ) {
				throw new AssertionFailure( "Internal error - preBuildIndexingProcessor should be called only once" );
			}
			this.indexingProcessor = this.indexingProcessorBuilder.build( dependencyCollector )
					.orElseGet( PojoIndexingProcessor::noOp );
			this.indexingProcessorBuilder = null;
		}

		public void preBuildIndexManager() {
			if ( this.indexManager != null ) {
				throw new AssertionFailure( "Internal error - preBuildIndexManager should be called only once" );
			}
			this.indexManager = this.indexManagerBuilder.build();
			this.indexManagerBuilder = null;
			extendedMappingCollector.indexManager( indexManager );
		}

		@Override
		public PojoIndexedTypeManager<?, E> build() {
			closed = true;
			return new PojoIndexedTypeManager<>( this, identifierMapping.mapping );
		}
	}
}
