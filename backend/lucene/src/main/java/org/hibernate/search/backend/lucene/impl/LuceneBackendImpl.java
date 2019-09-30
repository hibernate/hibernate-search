/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneReadWorkOrchestratorImplementor;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.index.impl.IndexManagerBackendContext;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerBuilder;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneReadWorkOrchestratorImpl;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public class LuceneBackendImpl implements BackendImplementor<LuceneRootDocumentBuilder>, LuceneBackend {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final BeanHolder<? extends DirectoryProvider> directoryProviderHolder;

	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private final LuceneReadWorkOrchestratorImplementor readOrchestrator;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final EventContext eventContext;
	private final IndexManagerBackendContext indexManagerBackendContext;

	LuceneBackendImpl(String name, BeanHolder<? extends DirectoryProvider> directoryProviderHolder,
			LuceneWorkFactory workFactory,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy,
			ErrorHandler errorHandler) {
		this.name = name;
		this.directoryProviderHolder = directoryProviderHolder;

		this.analysisDefinitionRegistry = analysisDefinitionRegistry;

		this.readOrchestrator = new LuceneReadWorkOrchestratorImpl(
				"Lucene read work orchestrator for backend " + name
		);
		this.multiTenancyStrategy = multiTenancyStrategy;

		this.eventContext = EventContexts.fromBackendName( name );
		this.indexManagerBackendContext = new IndexManagerBackendContext(
				eventContext, directoryProviderHolder.get(),
				workFactory, multiTenancyStrategy,
				analysisDefinitionRegistry,
				errorHandler,
				readOrchestrator
		);
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( name ).append( ", " )
				.append( "directoryProvider=" ).append( directoryProviderHolder.get() )
				.append( "]" )
				.toString();
	}

	@Override
	public void start(BackendStartContext context) {
		// TODO HSEARCH-3528 start thread(s) and allocate resources specific to this backend here
	}

	@Override
	public CompletableFuture<?> preStop() {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( LuceneReadWorkOrchestratorImplementor::stop, readOrchestrator );
			closer.push( holder -> holder.get().close(), directoryProviderHolder );
			closer.push( BeanHolder::close, directoryProviderHolder );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( LuceneBackend.class ) ) {
			return (T) this;
		}
		throw log.backendUnwrappingWithUnknownType(
				clazz, LuceneBackend.class, eventContext
		);
	}

	@Override
	public Backend toAPI() {
		return this;
	}

	@Override
	public IndexManagerBuilder<LuceneRootDocumentBuilder> createIndexManagerBuilder(
			String indexName, boolean multiTenancyEnabled, BackendBuildContext context, ConfigurationPropertySource propertySource) {
		if ( multiTenancyEnabled && !multiTenancyStrategy.isMultiTenancySupported() ) {
			throw log.multiTenancyRequiredButNotSupportedByBackend( indexName, eventContext );
		}

		LuceneIndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder = new LuceneIndexSchemaRootNodeBuilder(
				EventContexts.fromIndexName( indexName ), analysisDefinitionRegistry
		);

		/*
		 * We do not normalize index names: directory providers are expected to use the exact given index name,
		 * or a reversible conversion of that name, as an internal key (file names, ...),
		 * and therefore the internal key should stay unique.
		 */
		return new LuceneIndexManagerBuilder(
				indexManagerBackendContext,
				indexName, indexSchemaRootNodeBuilder
		);
	}
}
