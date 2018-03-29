/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.reader.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * Creates and closes the IndexReaders encompassing multiple indexes.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public final class MultiReaderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private MultiReaderFactory() {
		//not allowed
	}

	public static IndexReader openReader(Set<String> indexNames, Set<ReaderProvider> readerProviders) {
		if ( readerProviders.size() == 0 ) {
			return null;
		}
		else {
			//everything should be the same so wrap in an MultiReader
			try {
				return ManagedMultiReader.createInstance( readerProviders );
			}
			catch (IOException e) {
				throw log.ioExceptionOnMultiReaderRefresh( indexNames, e );
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
