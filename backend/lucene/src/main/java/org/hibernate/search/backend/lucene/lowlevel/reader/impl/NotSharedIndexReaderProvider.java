/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;

import org.apache.lucene.index.DirectoryReader;

/**
 * A simplistic index reader holder that opens a new index reader
 * from the directory every time an index reader is requested.
 */
public class NotSharedIndexReaderProvider implements IndexReaderProvider {

	private final DirectoryHolder directoryHolder;

	public NotSharedIndexReaderProvider(DirectoryHolder directoryHolder) {
		this.directoryHolder = directoryHolder;
	}

	@Override
	public void clear() {
		// Nothing to do
	}

	@Override
	public DirectoryReader getOrCreate() throws IOException {
		return DirectoryReader.open( directoryHolder.get() );
	}

}
