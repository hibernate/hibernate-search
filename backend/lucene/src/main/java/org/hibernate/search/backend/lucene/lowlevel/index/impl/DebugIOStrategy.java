/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.index.impl;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderProvider;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.NotSharedIndexReaderProvider;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterConfigSource;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;
import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.reporting.EventContext;

public class DebugIOStrategy extends IOStrategy {

	public static DebugIOStrategy create(DirectoryProvider directoryProvider, BackendThreads threads,
			FailureHandler failureHandler) {
		return new DebugIOStrategy( directoryProvider, threads, failureHandler );
	}

	private DebugIOStrategy(DirectoryProvider directoryProvider, BackendThreads threads,
			FailureHandler failureHandler) {
		super( directoryProvider, threads, failureHandler );
	}

	@Override
	IndexWriterProvider createIndexWriterProvider(String indexName, EventContext eventContext,
			DirectoryHolder directoryHolder, IndexWriterConfigSource configSource) {
		return new IndexWriterProvider(
				indexName, eventContext,
				directoryHolder, configSource,
				null, 0,
				threads,
				failureHandler
		);
	}

	@Override
	IndexReaderProvider createIndexReaderProvider(DirectoryHolder directoryHolder,
			IndexWriterProvider indexWriterProvider) {
		return new NotSharedIndexReaderProvider( directoryHolder );
	}

}
