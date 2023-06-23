/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.DocumentRouter;
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingIndexedTypeContext;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathOrdinals;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoDocumentContributor;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkIndexedTypeContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 */
public class PojoIndexedTypeManager<I, E> extends AbstractPojoTypeManager<I, E>
		implements PojoWorkIndexedTypeContext<I, E>, PojoScopeIndexedTypeContext<I, E>,
		PojoMassIndexingIndexedTypeContext<E> {
	private final DocumentRouter<? super E> documentRouter;
	private final PojoIndexingProcessor<E> processor;
	private final MappedIndexManager indexManager;

	public PojoIndexedTypeManager(String entityName, PojoRawTypeIdentifier<E> typeIdentifier,
			PojoCaster<E> caster, boolean singleConcreteTypeInEntityHierarchy,
			IdentifierMappingImplementor<I, E> identifierMapping,
			DocumentRouter<? super E> documentRouter, PojoPathOrdinals pathOrdinals,
			PojoIndexingProcessor<E> processor, MappedIndexManager indexManager,
			PojoImplicitReindexingResolver<E> reindexingResolver) {
		super( entityName, typeIdentifier, caster, singleConcreteTypeInEntityHierarchy,
				identifierMapping, pathOrdinals, reindexingResolver );
		this.documentRouter = documentRouter;
		this.processor = processor;
		this.indexManager = indexManager;
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
		appender.attribute( "entityName", entityName )
				.attribute( "typeIdentifier", typeIdentifier )
				.attribute( "indexManager", indexManager )
				.attribute( "identifierMapping", identifierMapping )
				.attribute( "documentRouter", documentRouter )
				.attribute( "processor", processor )
				.attribute( "reindexingResolver", reindexingResolver );
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
}
