/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.index.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.lowlevel.directory.impl.DirectoryCreationContextImpl;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderProvider;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;

public abstract class IOStrategy {

	private final DirectoryProvider directoryProvider;
	final ThreadPoolProvider threadPoolProvider;
	final FailureHandler failureHandler;

	protected IOStrategy(DirectoryProvider directoryProvider, ThreadPoolProvider threadPoolProvider,
			FailureHandler failureHandler) {
		this.directoryProvider = directoryProvider;
		this.threadPoolProvider = threadPoolProvider;
		this.failureHandler = failureHandler;
	}

	public IndexAccessorImpl createIndexAccessor(String indexName, EventContext eventContext,
			Optional<String> shardId, Analyzer analyzer) {
		DirectoryHolder directoryHolder;
		DirectoryCreationContext context = new DirectoryCreationContextImpl(
				shardId.isPresent() ? EventContexts.fromShardId( shardId.get() ) : null,
				indexName,
				shardId
		);
		directoryHolder = directoryProvider.createDirectoryHolder( context );
		IndexWriterProvider indexWriterProvider = null;
		IndexReaderProvider indexReaderProvider = null;
		try {
			indexWriterProvider = createIndexWriterProvider( indexName, eventContext, analyzer, directoryHolder );
			indexReaderProvider = createIndexReaderProvider( directoryHolder, indexWriterProvider );
			return new IndexAccessorImpl(
					eventContext,
					directoryHolder, indexWriterProvider, indexReaderProvider
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( IndexWriterProvider::clear, indexWriterProvider )
					.push( IndexReaderProvider::clear, indexReaderProvider )
					.push( directoryHolder );
			throw e;
		}
	}

	abstract IndexWriterProvider createIndexWriterProvider(String indexName, EventContext eventContext, Analyzer analyzer,
			DirectoryHolder directoryHolder);

	abstract IndexReaderProvider createIndexReaderProvider(DirectoryHolder directoryHolder,
			IndexWriterProvider indexWriterProvider);

}
