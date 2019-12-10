/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.spi;

import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;

final class SimpleDirectoryReaderHolder implements DirectoryReaderHolder {
	private final DirectoryReader directoryReader;

	SimpleDirectoryReaderHolder(DirectoryReader directoryReader) {
		this.directoryReader = directoryReader;
	}

	@Override
	public DirectoryReader get() {
		return directoryReader;
	}

	@Override
	public void close() throws IOException {
		directoryReader.close();
	}
}
