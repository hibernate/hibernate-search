//$Id$
package org.hibernate.search.reader;

import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Responsible for providing and managing the lifecycle of a read only reader. The implementation must have a
 * no-arg constructor.
 * <p/>
 * Note that the reader must be closed once opened.
 *
 * @author Emmanuel Bernard
 */
public interface ReaderProvider {
	/**
	 * Open a read-only reader on all the listed directory providers.
	 * The opened reader has to be closed through {@link #closeReader(IndexReader)}.
	 * The opening can be virtual.
	 */
	IndexReader openReader(DirectoryProvider... directoryProviders);

	/**
	 * Close a reader previously opened by {@link #openReader}.
	 * The closing can be virtual.
	 */
	void closeReader(IndexReader reader);

	/**
	 * Inialize the reader provider before its use.
	 */
	void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor);

	/**
	 * Called when a <code>SearchFactory</code> is destroyed. This method typically releases resources.
	 * It is guaranteed to be executed after readers are released by queries (assuming no user error). 
	 */
	void destroy();
}
