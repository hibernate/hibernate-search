/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.RoutingKeyProvider;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.work.impl.CachingCastingEntitySupplier;
import org.hibernate.search.mapper.pojo.work.impl.PojoDocumentContributor;
import org.hibernate.search.mapper.pojo.work.impl.PojoDocumentReferenceProvider;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexedTypeIndexingPlan;
import org.hibernate.search.mapper.pojo.work.impl.PojoTypeIndexer;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 * @param <D> The document type for the index.
 */
public class PojoIndexedTypeManager<I, E, D extends DocumentElement>
		implements AutoCloseable, ToStringTreeAppendable,
		PojoWorkIndexedTypeContext<I, E, D>, PojoScopeIndexedTypeContext<I, E, D> {

	private final Class<E> indexedJavaClass;
	private final PojoCaster<E> caster;
	private final IdentifierMappingImplementor<I, E> identifierMapping;
	private final RoutingKeyProvider<E> routingKeyProvider;
	private final PojoIndexingProcessor<E> processor;
	private final MappedIndexManager<D> indexManager;
	private final PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver;

	public PojoIndexedTypeManager(Class<E> indexedJavaClass,
			PojoCaster<E> caster,
			IdentifierMappingImplementor<I, E> identifierMapping,
			RoutingKeyProvider<E> routingKeyProvider,
			PojoIndexingProcessor<E> processor, MappedIndexManager<D> indexManager,
			PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver) {
		this.indexedJavaClass = indexedJavaClass;
		this.caster = caster;
		this.identifierMapping = identifierMapping;
		this.routingKeyProvider = routingKeyProvider;
		this.processor = processor;
		this.indexManager = indexManager;
		this.reindexingResolver = reindexingResolver;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[javaType = " + indexedJavaClass + "]";
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierMappingImplementor::close, identifierMapping );
			closer.push( RoutingKeyProvider::close, routingKeyProvider );
			closer.push( PojoIndexingProcessor::close, processor );
			closer.push( PojoImplicitReindexingResolver::close, reindexingResolver );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "indexedJavaClass", indexedJavaClass )
				.attribute( "indexManager", indexManager )
				.attribute( "identifierMapping", identifierMapping )
				.attribute( "routingKeyProvider", routingKeyProvider )
				.attribute( "processor", processor )
				.attribute( "reindexingResolver", reindexingResolver );
	}

	@Override
	public Class<E> getJavaClass() {
		return indexedJavaClass;
	}

	@Override
	public IdentifierMappingImplementor<I, E> getIdentifierMapping() {
		return identifierMapping;
	}

	@Override
	public Supplier<E> toEntitySupplier(AbstractPojoBackendSessionContext sessionContext, Object entity) {
		PojoRuntimeIntrospector introspector = sessionContext.getRuntimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, introspector, entity );
	}

	@Override
	public DocumentReferenceProvider toDocumentReferenceProvider(AbstractPojoBackendSessionContext sessionContext,
			I identifier, Supplier<E> entitySupplier) {
		String documentIdentifier = identifierMapping.toDocumentIdentifier(
				identifier, sessionContext.getMappingContext()
		);
		return new PojoDocumentReferenceProvider<>(
				routingKeyProvider, sessionContext,
				identifier, documentIdentifier, entitySupplier
		);
	}

	@Override
	public PojoDocumentContributor<D, E> toDocumentContributor(Supplier<E> entitySupplier, AbstractPojoBackendSessionContext sessionContext) {
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
	public PojoTypeIndexer<I, E, D> createIndexer(
			AbstractPojoBackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy) {
		return new PojoTypeIndexer<>(
				this, sessionContext,
				indexManager.createIndexer( sessionContext, commitStrategy )
		);
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(DetachedBackendSessionContext sessionContext) {
		return indexManager.createWorkExecutor( sessionContext );
	}

	@Override
	public PojoIndexedTypeIndexingPlan<I, E, D> createIndexingPlan(AbstractPojoBackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoIndexedTypeIndexingPlan<>(
				this, sessionContext,
				indexManager.createIndexingPlan( sessionContext, commitStrategy, refreshStrategy )
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
