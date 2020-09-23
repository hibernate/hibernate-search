/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.CachingCastingEntitySupplier;
import org.hibernate.search.mapper.pojo.work.impl.PojoDocumentContributor;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexedTypeIndexingPlan;
import org.hibernate.search.mapper.pojo.work.impl.PojoTypeIndexer;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkRouter;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 */
public class PojoIndexedTypeManager<I, E>
		implements AutoCloseable, ToStringTreeAppendable,
		PojoWorkIndexedTypeContext<I, E>, PojoScopeIndexedTypeContext<I, E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final PojoCaster<E> caster;
	private final IdentifierMappingImplementor<I, E> identifierMapping;
	private final BeanHolder<? extends RoutingBridge<? super E>> routingBridgeHolder;
	private final PojoIndexingProcessor<E> processor;
	private final MappedIndexManager indexManager;
	private final PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver;

	public PojoIndexedTypeManager(PojoRawTypeIdentifier<E> typeIdentifier,
			PojoCaster<E> caster,
			IdentifierMappingImplementor<I, E> identifierMapping,
			BeanHolder<? extends RoutingBridge<? super E>> routingBridgeHolder,
			PojoIndexingProcessor<E> processor, MappedIndexManager indexManager,
			PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver) {
		this.typeIdentifier = typeIdentifier;
		this.caster = caster;
		this.identifierMapping = identifierMapping;
		this.routingBridgeHolder = routingBridgeHolder;
		this.processor = processor;
		this.indexManager = indexManager;
		this.reindexingResolver = reindexingResolver;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[javaType = " + typeIdentifier + "]";
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierMappingImplementor::close, identifierMapping );
			closer.push( holder -> holder.get().close(), routingBridgeHolder );
			closer.push( BeanHolder::close, routingBridgeHolder );
			closer.push( PojoIndexingProcessor::close, processor );
			closer.push( PojoImplicitReindexingResolver::close, reindexingResolver );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "typeIdentifier", typeIdentifier )
				.attribute( "indexManager", indexManager )
				.attribute( "identifierMapping", identifierMapping )
				.attribute( "routingBridgeHolder", routingBridgeHolder )
				.attribute( "processor", processor )
				.attribute( "reindexingResolver", reindexingResolver );
	}

	@Override
	public PojoRawTypeIdentifier<E> typeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public IdentifierMappingImplementor<I, E> identifierMapping() {
		return identifierMapping;
	}

	@Override
	public Supplier<E> toEntitySupplier(PojoWorkSessionContext<?> sessionContext, Object entity) {
		PojoRuntimeIntrospector introspector = sessionContext.runtimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, introspector, entity );
	}

	@Override
	public String toDocumentIdentifier(PojoWorkSessionContext<?> sessionContext, I identifier) {
		return identifierMapping.toDocumentIdentifier( identifier, sessionContext.mappingContext() );
	}

	@Override
	public PojoWorkRouter createRouter(PojoWorkSessionContext<?> sessionContext, I identifier,
			Supplier<E> entitySupplier) {
		if ( routingBridgeHolder == null ) {
			return NoOpDocumentRouter.INSTANCE;
		}
		return new RoutingBridgeDocumentRouter<>( sessionContext.routingBridgeRouteContext(), routingBridgeHolder.get(),
				identifier, entitySupplier.get() );
	}

	@Override
	public PojoDocumentContributor<E> toDocumentContributor(Supplier<E> entitySupplier, PojoWorkSessionContext<?> sessionContext) {
		return new PojoDocumentContributor<>( processor, sessionContext, entitySupplier );
	}

	@Override
	public boolean requiresSelfReindexing(Set<String> dirtyPaths) {
		return reindexingResolver.requiresSelfReindexing( dirtyPaths );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoRuntimeIntrospector runtimeIntrospector,
			Supplier<E> entitySupplier, Set<String> dirtyPaths) {
		reindexingResolver.resolveEntitiesToReindex(
				collector, runtimeIntrospector, entitySupplier.get(), dirtyPaths
		);
	}

	@Override
	public IndexSchemaManager schemaManager() {
		return indexManager.schemaManager();
	}

	@Override
	public PojoTypeIndexer<I, E> createIndexer(PojoWorkSessionContext<?> sessionContext) {
		return new PojoTypeIndexer<>(
				this, sessionContext,
				indexManager.createIndexer(
						sessionContext
				)
		);
	}

	@Override
	public IndexWorkspace createWorkspace(DetachedBackendSessionContext sessionContext) {
		return indexManager.createWorkspace( sessionContext );
	}

	@Override
	public <R> PojoIndexedTypeIndexingPlan<I, E, R> createIndexingPlan(PojoWorkSessionContext<R> sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoIndexedTypeIndexingPlan<>(
				this, sessionContext,
				indexManager.createIndexingPlan(
						sessionContext, sessionContext.entityReferenceFactory(),
						commitStrategy, refreshStrategy
				)
		);
	}

	@Override
	public <R, E2> MappedIndexScopeBuilder<R, E2> createScopeBuilder(BackendMappingContext mappingContext) {
		return indexManager.createScopeBuilder( mappingContext );
	}

	@Override
	public void addTo(MappedIndexScopeBuilder<?, ?> builder) {
		indexManager.addTo( builder );
	}
}
