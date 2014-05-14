/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.reader.impl;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Wraps a MultiReader to keep references to owning managers.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ManagedMultiReader extends MultiReader {

	private static final Log log = LoggerFactory.make();

	final IndexReader[] subReaders;
	final ReaderProvider[] managers;

	public ManagedMultiReader(IndexReader[] subReaders, ReaderProvider[] managers) {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( subReaders, true );
		this.subReaders = subReaders;
		this.managers = managers;
		assert subReaders.length == managers.length;
	}

	@Override
	protected synchronized void doClose() throws IOException {
		/**
		 * Important: we don't really close the sub readers but we delegate to the
		 * close method of the managing ReaderProvider, which might reuse the same
		 * IndexReader.
		 */
		final boolean debugEnabled = log.isDebugEnabled();
		if ( debugEnabled ) {
			log.debugf( "Closing MultiReader: %s", this );
		}
		for ( int i = 0; i < subReaders.length; i++ ) {
			ReaderProvider container = managers[i];
			container.closeIndexReader( subReaders[i] ); // might be virtual
		}
		if ( debugEnabled ) {
			log.trace( "MultiReader closed." );
		}
	}

	@Override
	public String toString() {
		return "CacheableMultiReader [subReaders=" + Arrays.toString( subReaders ) + ", managers=" + Arrays.toString( managers ) + "]";
	}

}
