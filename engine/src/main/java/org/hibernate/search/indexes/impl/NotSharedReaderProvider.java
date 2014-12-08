/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Open a reader each time
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class NotSharedReaderProvider implements DirectoryBasedReaderProvider {

	private static final Log log = LoggerFactory.make();

	private DirectoryProvider directoryProvider;
	private String indexName;

	@Override
	public DirectoryReader openIndexReader() {
		// #getDirectory must be invoked each time as the underlying directory might "dance" as in
		// org.hibernate.search.store.impl.FSSlaveDirectoryProvider
		Directory directory = directoryProvider.getDirectory();
		try {
			return DirectoryReader.open( directory );
		}
		catch (IOException e) {
			throw new SearchException( "Could not open index \"" + indexName + "\"", e );
		}
	}

	@Override
	public void closeIndexReader(IndexReader reader) {
		try {
			reader.close();
		}
		catch (IOException e) {
			log.unableToCloseLuceneIndexReader( e );
		}
	}

	@Override
	public void initialize(DirectoryBasedIndexManager indexManager, Properties props) {
		directoryProvider = indexManager.getDirectoryProvider();
		indexName = indexManager.getIndexName();
	}

	@Override
	public void stop() {
		//nothing to do for this implementation
	}

}
