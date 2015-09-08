/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.reader.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.hibernate.search.engine.metadata.impl.SortFieldMetadata;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Creates and closes the IndexReaders encompassing multiple indexes.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public final class MultiReaderFactory {

	private static final Log log = LoggerFactory.make();

	private MultiReaderFactory() {
		//not allowed
	}

	public static IndexReader openReader(IndexManager... indexManagers) {
		return openReader( null, null, indexManagers );
	}

	public static IndexReader openReader(Iterable<SortFieldMetadata> configuredSortFields, Sort sort, IndexManager[] indexManagers) {
		final int length = indexManagers.length;
		IndexReader[] readers = new IndexReader[length];
		ReaderProvider[] managers = new ReaderProvider[length];
		for ( int index = 0; index < length; index++ ) {
			ReaderProvider indexReaderManager = indexManagers[index].getReaderProvider();
			IndexReader openIndexReader = indexReaderManager.openIndexReader();
			readers[index] = openIndexReader;
			managers[index] = indexReaderManager;
		}

		if ( length == 0 ) {
			return null;
		}
		else {
			//everything should be the same so wrap in an MultiReader
			try {
				return ManagedMultiReader.createInstance( readers, managers, configuredSortFields, sort );
			}
			catch (IOException e) {
				throw log.ioExceptionOnMultiReaderRefresh( e );
			}
		}
	}

	public static void closeReader(IndexReader multiReader) {
		if ( multiReader == null ) {
			return;
		}
		try {
			//This used to be more complex, complexity is now hidden into
			//org.hibernate.search.reader.impl.ManagedMultiReader#doClose()
			multiReader.close();
		}
		catch (IOException e) {
			//The implementation of IndexReader we use would actually never throw
			//an IOException, so just in case user is passing a custom IndexReader.
			log.couldNotCloseResource( e );
		}
	}

}
