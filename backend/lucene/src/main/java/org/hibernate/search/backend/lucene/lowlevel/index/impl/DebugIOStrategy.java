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
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;

public class DebugIOStrategy extends IOStrategy {

	public static DebugIOStrategy create(DirectoryProvider directoryProvider, ThreadPoolProvider threadPoolProvider,
			FailureHandler failureHandler) {
		return new DebugIOStrategy( directoryProvider, threadPoolProvider, failureHandler );
	}

	private DebugIOStrategy(DirectoryProvider directoryProvider, ThreadPoolProvider threadPoolProvider,
			FailureHandler failureHandler) {
		super( directoryProvider, threadPoolProvider, failureHandler );
	}

	@Override
	IndexReaderProvider createIndexReaderProvider(DirectoryHolder directoryHolder,
			IndexWriterProvider indexWriterProvider) {
		return new NotSharedIndexReaderProvider( directoryHolder );
	}

}
