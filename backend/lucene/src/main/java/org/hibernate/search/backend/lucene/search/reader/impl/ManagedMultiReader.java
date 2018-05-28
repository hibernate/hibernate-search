/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.reader.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * Wraps a MultiReader to keep references to owning managers.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Guillaume Smet
 */
public class ManagedMultiReader extends MultiReader {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final ReaderProvider[] readerProviders;


	private ManagedMultiReader(IndexReader[] subReaders, ReaderProvider[] readerProviders) throws IOException {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( subReaders, true );
		this.readerProviders = readerProviders;
	}

	static ManagedMultiReader createInstance(Set<ReaderProvider> readerProviders) throws IOException {
		IndexReader[] indexReaders = readerProviders.stream()
				.map( ReaderProvider::openIndexReader )
				.toArray( size -> new IndexReader[size] );

		return new ManagedMultiReader( indexReaders, readerProviders.toArray( new ReaderProvider[readerProviders.size()] ) );
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
		for ( int i = 0; i < getSequentialSubReaders().size(); i++ ) {
			ReaderProvider container = readerProviders[i];
			container.closeIndexReader( getSequentialSubReaders().get( i ) );
		}
		if ( debugEnabled ) {
			log.trace( "MultiReader closed." );
		}
	}

	// Exposed only for testing
	public List<? extends IndexReader> getSubReaders() {
		return getSequentialSubReaders();
	}

	@Override
	public String toString() {
		return ManagedMultiReader.class.getSimpleName() + " [subReaders=" + getSequentialSubReaders() + ", readerProviders=" + Arrays.toString( readerProviders ) + "]";
	}
}
