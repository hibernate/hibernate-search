/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;
import java.nio.file.Path;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

public class MMapDirectoryProvider implements DirectoryProvider {
	private final EventContext backendContext;

	private final Path rootDirectory;

	public MMapDirectoryProvider(EventContext backendContext, Path rootDirectory) {
		this.backendContext = backendContext;
		this.rootDirectory = rootDirectory;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() +
				"[" +
				"rootDirectory=" + rootDirectory +
				"]";
	}

	@Override
	public Directory createDirectory(String indexName) throws IOException {
		EventContext indexContext = backendContext.append( EventContexts.fromIndexName( indexName ) );
		Path directoryPath = rootDirectory.resolve( indexName );
		DirectoryHelper.makeSanityCheckedFilesystemDirectory( directoryPath, indexContext );
		Directory directory = new MMapDirectory( directoryPath );
		try {
			DirectoryHelper.initializeIndexIfNeeded( directory, indexContext );
		}
		catch (IOException | RuntimeException e) {
			new SuppressingCloser( e ).push( directory );
			throw e;
		}
		return directory;
	}
}
