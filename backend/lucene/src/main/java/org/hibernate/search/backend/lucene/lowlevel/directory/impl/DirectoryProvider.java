/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;

import org.apache.lucene.store.Directory;

// TODO HSEARCH-3440 make this an SPI, provide multiple implementations
public interface DirectoryProvider {

	// TODO HSEARCH-3440 return an SPI type so that people can easily add behavior to the close() method
	// (could be useful if they started a thread pool when creating the directory for instance)
	/**
	 * Create a {@link Directory} for a given name, allocating internal resources (filesystem directories, ...)
	 * as necessary.
	 * <p>
	 * The provided index names are raw and do not take into account the limitations of the internal representation
	 * of indexes. If some characters cannot be used in a given {@link DirectoryProvider},
	 * this provider is expected to escape characters as necessary using a encoding scheme attributing
	 * a unique representation to each index name,
	 * so as to avoid two index names to be encoded into identical internal representations.
	 * Lower-casing the index name, for example, is not an acceptable encoding scheme,
	 * as two index names differing only in case could end up using the same directory.
	 *
	 * @param indexName The name of the index in Hibernate Search.
	 * @return The directory to use for that index name
	 * @throws IOException If an error occurs while initializing the directory.
	 */
	Directory createDirectory(String indexName) throws IOException;

}
