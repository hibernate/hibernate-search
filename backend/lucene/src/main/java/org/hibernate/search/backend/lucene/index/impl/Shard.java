/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.DirectoryCreationContextImpl;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IOStrategy;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestratorImpl;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestratorImpl;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.resources.spi.SavedState;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.DirectoryReader;

public final class Shard {

	private static final ConfigurationProperty<BeanReference<? extends DirectoryProvider>> DIRECTORY_TYPE =
			ConfigurationProperty.forKey( LuceneIndexSettings.DIRECTORY_TYPE )
					.asBeanReference( DirectoryProvider.class )
					.withDefault( BeanReference.of( DirectoryProvider.class, LuceneIndexSettings.Defaults.DIRECTORY_TYPE ) )
					.build();

	private static final SavedState.Key<DirectoryHolder> DIRECTORY_HOLDER_KEY = SavedState.key( "directory_holder" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Optional<String> shardId;
	private final IndexManagerBackendContext backendContext;
	private final LuceneIndexModel model;

	private DirectoryHolder directoryHolder;
	private IndexAccessorImpl indexAccessor;
	private LuceneParallelWorkOrchestratorImpl managementOrchestrator;
	private LuceneSerialWorkOrchestratorImpl indexingOrchestrator;

	private boolean savedForRestart = false;

	Shard(Optional<String> shardId, IndexManagerBackendContext backendContext, LuceneIndexModel model) {
		this.shardId = shardId;
		this.backendContext = backendContext;
		this.model = model;
	}

	public SavedState saveForRestart() {
		try {
			return SavedState.builder()
					.put( DIRECTORY_HOLDER_KEY, directoryHolder, DirectoryHolder::close )
					.build();
		}
		finally {
			savedForRestart = true;
		}
	}

	void preStart(ConfigurationPropertySource propertySource, BeanResolver beanResolver, SavedState savedState) {
		Optional<DirectoryHolder> savedDirectoryHolder = savedState.get( Shard.DIRECTORY_HOLDER_KEY );
		try {
			if ( savedDirectoryHolder.isPresent() ) {
				directoryHolder = savedDirectoryHolder.get();
			}
			else {
				try ( BeanHolder<? extends DirectoryProvider> directoryProviderHolder =
						DIRECTORY_TYPE.getAndTransform( propertySource, beanResolver::resolve ) ) {
					String indexName = model.hibernateSearchName();
					EventContext indexAndShardEventContext = EventContexts.fromIndexNameAndShardId( indexName, shardId );
					DirectoryCreationContext context = new DirectoryCreationContextImpl( indexAndShardEventContext,
							indexName, shardId, beanResolver,
							propertySource.withMask( "directory" ) );
					directoryHolder = directoryProviderHolder.get().createDirectoryHolder( context );
				}
				directoryHolder.start();
			}
		}
		catch (IOException | RuntimeException e) {
			throw log.unableToStartShard( e.getMessage(), e );
		}
	}

	void start(ConfigurationPropertySource propertySource) {
		String indexName = model.hibernateSearchName();
		EventContext indexAndShardEventContext = EventContexts.fromIndexNameAndShardId( indexName, shardId );
		try {
			IOStrategy ioStrategy = backendContext.createIOStrategy( propertySource );
			indexAccessor = backendContext.createIndexAccessor( model, indexAndShardEventContext, directoryHolder,
					ioStrategy, propertySource );
			managementOrchestrator =
					backendContext.createIndexManagementOrchestrator( indexAndShardEventContext, indexAccessor );
			indexingOrchestrator =
					backendContext.createIndexingOrchestrator( indexAndShardEventContext, indexAccessor );

			managementOrchestrator.start( propertySource );
			indexingOrchestrator.start( propertySource );
		}
		catch (RuntimeException e) {
			throw log.unableToStartShard( e.getMessage(), e );
		}
	}

	CompletableFuture<?> preStop() {
		return indexingOrchestrator.preStop();
	}

	void stop() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( LuceneSerialWorkOrchestratorImpl::stop, indexingOrchestrator );
			closer.push( LuceneParallelWorkOrchestratorImpl::stop, managementOrchestrator );
			// Close the index writer after the orchestrators, when we're sure all works have been performed
			closer.push( IndexAccessorImpl::close, indexAccessor );
			if ( !savedForRestart ) {
				closer.push( DirectoryHolder::close, directoryHolder );
			}
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToShutdownShard(
					e.getMessage(),
					shardId.map( EventContexts::fromShardId ).orElse( null ),
					e
			);
		}
	}

	DirectoryReader openReader() throws IOException {
		return indexAccessor.getIndexReader();
	}

	LuceneSerialWorkOrchestrator indexingOrchestrator() {
		return indexingOrchestrator;
	}

	LuceneParallelWorkOrchestrator managementOrchestrator() {
		return managementOrchestrator;
	}

	public IndexAccessorImpl indexAccessorForTests() {
		return indexAccessor;
	}
}
