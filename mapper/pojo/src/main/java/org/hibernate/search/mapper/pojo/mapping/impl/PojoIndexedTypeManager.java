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
import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoReindexingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 * @param <D> The document type for the index.
 */
public class PojoIndexedTypeManager<I, E, D extends DocumentElement> implements AutoCloseable, ToStringTreeAppendable {

	private final Class<E> indexedJavaClass;
	private final PojoCaster<E> caster;
	private final IdentifierMapping<I, E> identifierMapping;
	private final RoutingKeyProvider<E> routingKeyProvider;
	private final PojoIndexingProcessor<E> processor;
	private final MappedIndexManager<D> indexManager;
	private final PojoImplicitReindexingResolver<E, Set<String>> reindexingResolver;

	public PojoIndexedTypeManager(Class<E> indexedJavaClass,
			PojoCaster<E> caster,
			IdentifierMapping<I, E> identifierMapping,
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
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierMapping::close, identifierMapping );
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

	public PojoTypeDocumentWorkExecutor<I, E, D> createDocumentWorkExecutor(
			AbstractPojoSessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy) {
		return new PojoTypeDocumentWorkExecutor<>(
				this, sessionContext,
				indexManager.createDocumentWorkExecutor( sessionContext, commitStrategy )
		);
	}

	public IndexWorkExecutor createWorkExecutor() {
		return indexManager.createWorkExecutor();
	}

	IdentifierMapping<I, E> getIdentifierMapping() {
		return identifierMapping;
	}

	Class<E> getIndexedJavaClass() {
		return indexedJavaClass;
	}

	Supplier<E> toEntitySupplier(AbstractPojoSessionContextImplementor sessionContext, Object entity) {
		PojoRuntimeIntrospector introspector = sessionContext.getRuntimeIntrospector();
		return new CachingCastingEntitySupplier<>( caster, introspector, entity );
	}

	DocumentReferenceProvider toDocumentReferenceProvider(AbstractPojoSessionContextImplementor sessionContext,
			I identifier, Supplier<E> entitySupplier) {
		String documentIdentifier = identifierMapping.toDocumentIdentifier(
				identifier, sessionContext.getMappingContext()
		);
		return new PojoDocumentReferenceProvider<>(
				routingKeyProvider, sessionContext,
				identifier, documentIdentifier, entitySupplier
		);
	}

	PojoDocumentContributor<D, E> toDocumentContributor(Supplier<E> entitySupplier, AbstractPojoSessionContextImplementor sessionContext) {
		return new PojoDocumentContributor<>( processor, sessionContext, entitySupplier );
	}

	boolean requiresSelfReindexing(Set<String> dirtyPaths) {
		return reindexingResolver.requiresSelfReindexing( dirtyPaths );
	}

	void resolveEntitiesToReindex(PojoReindexingCollector collector, PojoRuntimeIntrospector runtimeIntrospector,
			Supplier<E> entitySupplier, Set<String> dirtyPaths) {
		reindexingResolver.resolveEntitiesToReindex(
				collector, runtimeIntrospector, entitySupplier.get(), dirtyPaths
		);
	}

	PojoIndexedTypeWorkPlan<I, E, D> createWorkPlan(AbstractPojoSessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoIndexedTypeWorkPlan<>(
				this, sessionContext,
				indexManager.createWorkPlan( sessionContext, commitStrategy, refreshStrategy )
		);
	}

	<R, E2> MappedIndexScopeBuilder<R, E2> createScopeBuilder(MappingContextImplementor mappingContext) {
		return indexManager.createScopeBuilder( mappingContext );
	}

	void addTo(MappedIndexScopeBuilder<?, ?> builder) {
		indexManager.addTo( builder );
	}
}
