/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoImplicitReindexingResolverBuildingHelper;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.NoOpDocumentRouter;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.RoutingBridgeDocumentRouter;
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.identity.impl.PojoRootIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.identity.impl.IdentityMappingMode;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManagerContainer;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIndexingProcessorOriginalTypeNodeBuilder;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoIndexedTypeManagerBuilder<E> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String entityName;
	private final PojoRawTypeModel<E> typeModel;

	private final MappedIndexManagerBuilder indexManagerBuilder;
	private final PojoIndexedTypeExtendedMappingCollector extendedMappingCollector;

	private final PojoRootIdentityMappingCollector<E> identityMappingCollector;
	private final BoundRoutingBridge<E> routingBridge;
	private final PojoIndexingProcessorOriginalTypeNodeBuilder<E> processorBuilder;

	private PojoIndexingProcessor<E> preBuiltIndexingProcessor;

	private boolean closed = false;

	PojoIndexedTypeManagerBuilder(String entityName, PojoRawTypeModel<E> typeModel,
			PojoMappingHelper mappingHelper,
			MappedIndexManagerBuilder indexManagerBuilder,
			PojoIndexedTypeExtendedMappingCollector extendedMappingCollector,
			BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge,
			BoundRoutingBridge<E> routingBridge,
			BeanResolver beanResolver) {
		this.entityName = entityName;
		this.typeModel = typeModel;
		this.indexManagerBuilder = indexManagerBuilder;
		this.extendedMappingCollector = extendedMappingCollector;
		this.identityMappingCollector = new PojoRootIdentityMappingCollector<>(
				typeModel,
				mappingHelper,
				Optional.of( indexManagerBuilder.rootBindingContext() ),
				providedIdentifierBridge,
				beanResolver
		);
		this.routingBridge = routingBridge;
		this.processorBuilder = new PojoIndexingProcessorOriginalTypeNodeBuilder<>(
				BoundPojoModelPath.root( typeModel ),
				mappingHelper, indexManagerBuilder.rootBindingContext(),
				identityMappingCollector,
				Collections.emptyList()
		);
	}

	void closeOnFailure() {
		if ( closed ) {
			return;
		}

		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoIndexingProcessorOriginalTypeNodeBuilder::closeOnFailure, processorBuilder );
			closer.push( PojoRootIdentityMappingCollector::closeOnFailure, identityMappingCollector );
			closer.push( RoutingBridge::close, routingBridge, BoundRoutingBridge::getBridge );
			closer.push( BeanHolder::close, routingBridge, BoundRoutingBridge::getBridgeHolder );
			closer.push( PojoIndexingProcessor::close, preBuiltIndexingProcessor );
			closed = true;
		}
	}

	PojoIndexMappingCollectorTypeNode asCollector() {
		return processorBuilder;
	}

	void preBuild(PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper) {
		if ( preBuiltIndexingProcessor != null ) {
			throw new AssertionFailure( "Internal error - preBuild should be called only once" );
		}

		PojoIndexingDependencyCollectorTypeNode<E> dependencyCollector =
				reindexingResolverBuildingHelper.createDependencyCollector( typeModel );

		if ( routingBridge != null ) {
			routingBridge.contributeDependencies( dependencyCollector );
		}

		preBuiltIndexingProcessor = processorBuilder.build( dependencyCollector )
				.orElseGet( PojoIndexingProcessor::noOp );
	}

	void buildAndAddTo(PojoTypeManagerContainer.Builder typeManagerContainerBuilder,
			PojoImplicitReindexingResolverBuildingHelper reindexingResolverBuildingHelper) {
		if ( preBuiltIndexingProcessor == null ) {
			throw new AssertionFailure( "Internal error - preBuild should be called before buildAndAddTo" );
		}

		IdentifierMappingImplementor<?, E> identifierMapping = identityMappingCollector
				.buildAndContributeTo( extendedMappingCollector, IdentityMappingMode.REQUIRED );

		PojoImplicitReindexingResolver<E> reindexingResolver =
				reindexingResolverBuildingHelper.build( typeModel );

		extendedMappingCollector.dirtyFilter( reindexingResolver.dirtySelfOrContainingFilter() );
		extendedMappingCollector.dirtyContainingAssociationFilter(
				reindexingResolver.associationInverseSideResolver().dirtyContainingAssociationFilter() );

		MappedIndexManager indexManager = indexManagerBuilder.build();
		extendedMappingCollector.indexManager( indexManager );


		PojoIndexedTypeManager<?, E> typeManager = new PojoIndexedTypeManager<>(
				entityName, typeModel.typeIdentifier(), typeModel.caster(),
				reindexingResolverBuildingHelper.isSingleConcreteTypeInEntityHierarchy( typeModel ),
				identifierMapping,
				routingBridge == null ? NoOpDocumentRouter.INSTANCE
						: new RoutingBridgeDocumentRouter<>( routingBridge.getBridgeHolder() ),
				reindexingResolverBuildingHelper.runtimePathsBuildingHelper( typeModel ).pathOrdinals(),
				preBuiltIndexingProcessor,
				indexManager,
				reindexingResolver
		);
		log.indexedTypeManager( typeModel, typeManager );

		typeManagerContainerBuilder.addIndexed( typeModel, typeManager );

		closed = true;
	}
}