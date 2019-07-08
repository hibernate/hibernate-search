/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.spi;

import java.io.IOException;

import org.apache.lucene.store.Directory;

final class SimpleDirectoryHolder implements DirectoryHolder {
	private final Directory directory;

	SimpleDirectoryHolder(Directory directory) {
		this.directory = directory;
	}

	@Override
	public Directory get() {
		return directory;
	}

	@Override
	public void close() throws IOException {
		directory.close();
	}
}
