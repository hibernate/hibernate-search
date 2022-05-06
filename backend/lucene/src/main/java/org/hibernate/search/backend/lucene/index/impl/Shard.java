/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestratorImpl;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestratorImpl;
import org.hibernate.search.engine.common.resources.spi.SavedState;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.DirectoryReader;

public final class Shard {

	static final SavedState.Key<DirectoryHolder> DIRECTORY_HOLDER_KEY = SavedState.key( "directory_holder" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext eventContext;
	private final IndexAccessorImpl indexAccessor;
	private final LuceneParallelWorkOrchestratorImpl managementOrchestrator;
	private final LuceneSerialWorkOrchestratorImpl indexingOrchestrator;
	private final boolean reuseAlreadyStaredDirectoryHolder;

	private boolean savedForRestart = false;

	Shard(EventContext eventContext, IndexAccessorImpl indexAccessor,
			LuceneParallelWorkOrchestratorImpl managementOrchestrator,
			LuceneSerialWorkOrchestratorImpl indexingOrchestrator,
			boolean reuseAlreadyStaredDirectoryHolder) {
		this.eventContext = eventContext;
		this.indexAccessor = indexAccessor;
		this.managementOrchestrator = managementOrchestrator;
		this.indexingOrchestrator = indexingOrchestrator;
		this.reuseAlreadyStaredDirectoryHolder = reuseAlreadyStaredDirectoryHolder;
	}

	public SavedState saveForRestart() {
		try {
			return SavedState.builder()
					.put( DIRECTORY_HOLDER_KEY, indexAccessor.directoryHolder(), DirectoryHolder::close )
					.build();
		}
		finally {
			savedForRestart = true;
		}
	}

	void start(ConfigurationPropertySource propertySource) {
		try {
			if ( !reuseAlreadyStaredDirectoryHolder ) {
				indexAccessor.start();
			}
			managementOrchestrator.start( propertySource );
			indexingOrchestrator.start( propertySource );
		}
		catch (IOException | RuntimeException e) {
			new SuppressingCloser( e )
					.push( indexAccessor )
					.push( LuceneSerialWorkOrchestratorImpl::stop, indexingOrchestrator )
					.push( LuceneParallelWorkOrchestratorImpl::stop, managementOrchestrator );
			throw log.unableToInitializeIndexDirectory(
					e.getMessage(),
					eventContext,
					e
			);
		}
	}

	CompletableFuture<?> preStop() {
		return indexingOrchestrator.preStop();
	}

	void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( LuceneSerialWorkOrchestratorImpl::stop, indexingOrchestrator );
			closer.push( LuceneParallelWorkOrchestratorImpl::stop, managementOrchestrator );
			// Close the index writer after the orchestrators, when we're sure all works have been performed
			if ( savedForRestart ) {
				closer.push( IndexAccessorImpl::closeNoDir, indexAccessor );
			}
			else {
				closer.push( IndexAccessorImpl::close, indexAccessor );
			}
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
