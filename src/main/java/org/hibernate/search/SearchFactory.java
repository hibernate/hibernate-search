//$Id$
package org.hibernate.search;

import org.apache.lucene.analysis.Analyzer;
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
	 * Provide access to the DirectoryProviders (hence the Lucene Directories)
	 * for a given entity
	 * In most cases, the returned type will be a one element array.
	 * But if the given entity is configured to use sharded indexes, then multiple
	 * elements will be returned. In this case all of them should be considered.
	 */
	DirectoryProvider[] getDirectoryProviders(Class<?> entity);

	/**
	 * Optimize all indexes
	 */
	void optimize();

	/**
	 * Optimize the index holding <code>entityType</code>
	 */
	void optimize(Class entityType);

	/**
	 * Experimental API
	 * retrieve an analyzer instance by its definition name
	 * 
	 * @throws SearchException if the definition name is unknown
	 */
	Analyzer getAnalyzer(String name);
	
	/**
	 * Retrieves the scoped analyzer for a given class.
	 * 
	 * @param clazz The class for which to retrieve the analyzer.
	 * @return The scoped analyzer for the specified class.
	 * @throws IllegalArgumentException in case <code>clazz == null</code> or the specified
	 * class is not an indexed entity.
	 */
	Analyzer getAnalyzer(Class<?> clazz);
}
