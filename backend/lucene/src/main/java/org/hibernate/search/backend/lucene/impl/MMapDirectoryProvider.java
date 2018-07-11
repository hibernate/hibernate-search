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
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

class MMapDirectoryProvider implements DirectoryProvider {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext backendContext;

	private final Path rootDirectory;

	public MMapDirectoryProvider(EventContext backendContext, Path rootDirectory) {
		this.backendContext = backendContext;
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
				throw log.localDirectoryIndexRootDirectoryNotWritableDirectory( indexDirectory, backendContext );
			}
		}
		else {
			try {
				Files.createDirectories( indexDirectory );
			}
			catch (Exception e) {
				throw log.unableToCreateIndexRootDirectoryForLocalDirectoryBackend( indexDirectory, backendContext, e );
			}
		}
	}
}
