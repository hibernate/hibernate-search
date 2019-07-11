/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.reader.spi.IndexReaderHolder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A {@link MultiReader} keeping references to {@link IndexReaderHolder}s to eventually close them.
 * <p>
 * Ported from Search 5: {@code org.hibernate.search.reader.impl.ManagedMultiReader},
 * {@code org.hibernate.search.reader.impl.MultiReaderFactory}.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class HolderMultiReader extends MultiReader {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static HolderMultiReader open(Set<String> indexNames,
			Set<? extends ReadIndexManagerContext> indexManagerContexts) {
		if ( indexManagerContexts.isEmpty() ) {
			return null;
		}
		else {
			List<IndexReaderHolder> indexReaderHolders = new ArrayList<>();
			try {
				for ( ReadIndexManagerContext indexManagerContext : indexManagerContexts ) {
					indexReaderHolders.add( indexManagerContext.openIndexReader() );
				}
				return new HolderMultiReader( indexReaderHolders );
			}
			catch (IOException | RuntimeException e) {
				new SuppressingCloser( e )
						.pushAll( indexReaderHolders );
				throw log.failureOnMultiReaderRefresh(
						EventContexts.fromIndexNames( indexNames ), e
				);
			}
		}
	}

	private final List<IndexReaderHolder> indexReaderHolders;

	HolderMultiReader(List<IndexReaderHolder> indexReaderHolders) throws IOException {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( toReaderArray( indexReaderHolders ), true );
		this.indexReaderHolders = indexReaderHolders;
	}

	@Override
	public String toString() {
		return HolderMultiReader.class.getSimpleName() + " [subReaders=" + getSequentialSubReaders()
				+ ", indexReaderHolders=" + indexReaderHolders + "]";
	}

	@Override
	protected synchronized void doClose() throws IOException {
		/*
		 * Important: we don't really close the sub readers but we delegate to the
		 * close method of the managing IndexReaderHolder.
		 * This method may decrement a usage counter instead of actually closing the reader,
		 * in cases where the reader is shared.
		 */
		final boolean debugEnabled = log.isDebugEnabled();
		if ( debugEnabled ) {
			log.debugf( "Closing MultiReader: %s", this );
		}
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.pushAll( IndexReaderHolder::close, indexReaderHolders );
		}
		if ( debugEnabled ) {
			log.trace( "MultiReader closed." );
		}
	}

	private static IndexReader[] toReaderArray(List<IndexReaderHolder> indexReaderHolders) {
		IndexReader[] indexReaders = new IndexReader[indexReaderHolders.size()];
		for ( int i = 0; i < indexReaderHolders.size(); i++ ) {
			indexReaders[i] = indexReaderHolders.get( i ).get();
		}
		return indexReaders;
	}
}
