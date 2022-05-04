/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockFactory;

final class LocalHeapDirectoryHolder implements DirectoryHolder {

	private final LockFactory lockFactory;

	private Directory directory;

	LocalHeapDirectoryHolder(LockFactory lockFactory) {
		this.lockFactory = lockFactory;
	}

	@Override
	public void start() {
		directory = new ByteBuffersDirectory( lockFactory );
	}

	@Override
	public void close() throws IOException {
		if ( directory != null ) {
			directory.close();
		}
		directory = null;
	}

	@Override
	public Directory get() {
		return directory;
	}
}
