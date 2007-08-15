//$Id: $
package org.hibernate.search;

import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Provide application wide operations as well as access to the underlying Lucene resources.
 * @author Emmanuel Bernard
 */
public interface SearchFactory {
	/**
	 * Provide the configured readerProvider strategy,
	 * hence access to a Lucene IndexReader
	 */
	ReaderProvider getReaderProvider();

	/**
	 * Provide access to the DirectoryProvider (hence the Lucene Directory)
	 * for a given entity
	 */
	DirectoryProvider[] getDirectoryProviders(Class entity);

	/**
	 * Optimize all indexes
	 */
	void optimize();

	/**
	 * Optimize the index holding <code>entityType</code>
	 */
	void optimize(Class entityType);
}
