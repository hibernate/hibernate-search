/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;

import org.apache.lucene.store.Directory;

// TODO make this an SPI, provide multiple implementations
public interface DirectoryProvider {

	// TODO return an SPI type so that people can easily add behavior to the close() method
	// (could be useful if they started a thread pool when creating the directory for instance)
	Directory createDirectory(String indexName) throws IOException;

}
