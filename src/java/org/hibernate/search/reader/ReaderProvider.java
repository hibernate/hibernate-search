//$Id$
package org.hibernate.search.reader;

import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Responsible for providing and managing the lifecycle of a read only reader
 * <p/>
 * Not that the reader must be closed once opened.
 *
 * The ReaderProvider implementation must have a no-arg constructor
 *
 * @author Emmanuel Bernard
 */
public interface ReaderProvider {
	/**
	 * Open a reader on all the listed directory providers
	 * the opened reader has to be closed through #closeReader()
	 * The opening can be virtual
	 */
	IndexReader openReader(DirectoryProvider... directoryProviders);

	/**
	 * close a reader previously opened by #openReader
	 * The closing can be virtual
	 */
	void closeReader(IndexReader reader);

	void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor);
}
