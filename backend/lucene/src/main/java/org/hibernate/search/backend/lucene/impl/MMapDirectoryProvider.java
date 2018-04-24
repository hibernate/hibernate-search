/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.index.impl.DirectoryProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

class MMapDirectoryProvider implements DirectoryProvider {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// TODO use a dedicated object for the error context instead of the backend
	private final BackendImplementor<?> backend;

	private final Path rootDirectory;

	public MMapDirectoryProvider(BackendImplementor<?> backend, Path rootDirectory) {
		this.backend = backend;
		this.rootDirectory = rootDirectory;
	}

	@Override
	public Directory createDirectory(String indexName) throws IOException {
		Path directoryPath = rootDirectory.resolve( indexName );
		initializeIndexDirectory( directoryPath );
		return new MMapDirectory( directoryPath );
	}

	private void initializeIndexDirectory(Path indexDirectory) {
		if ( Files.exists( indexDirectory ) ) {
			if ( !Files.isDirectory( indexDirectory ) || !Files.isWritable( indexDirectory ) ) {
				throw log.localDirectoryIndexRootDirectoryNotWritableDirectory( backend, indexDirectory );
			}
		}
		else {
			try {
				Files.createDirectories( indexDirectory );
			}
			catch (Exception e) {
				throw log.unableToCreateIndexRootDirectoryForLocalDirectoryBackend( backend, indexDirectory, e );
			}
		}
	}
}
