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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.reader.spi.DirectoryReaderHolder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A {@link MultiReader} keeping references to {@link DirectoryReaderHolder}s to eventually close them.
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
			Set<? extends ReadIndexManagerContext> indexManagerContexts, Set<String> routingKeys) {
		if ( indexManagerContexts.isEmpty() ) {
			return null;
		}
		else {
			Builder builder = new Builder();
			try {
				for ( ReadIndexManagerContext indexManagerContext : indexManagerContexts ) {
					indexManagerContext.openIndexReaders( routingKeys, builder );
				}
				return builder.build();
			}
			catch (IOException | RuntimeException e) {
				new SuppressingCloser( e )
						.pushAll( builder.directoryReaderHolders );
				throw log.failureOnMultiReaderRefresh(
						EventContexts.fromIndexNames( indexNames ), e
				);
			}
		}
	}

	private final List<DirectoryReaderHolder> directoryReaderHolders;
	private final IndexReaderMetadataResolver metadataResolver;

	HolderMultiReader(List<DirectoryReaderHolder> directoryReaderHolders, IndexReaderMetadataResolver metadataResolver) throws IOException {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( toReaderArray( directoryReaderHolders ), true );
		this.directoryReaderHolders = directoryReaderHolders;
		this.metadataResolver = metadataResolver;
	}

	@Override
	public String toString() {
		return HolderMultiReader.class.getSimpleName() + " [subReaders=" + getSequentialSubReaders()
				+ ", indexReaderHolders=" + directoryReaderHolders + "]";
	}

	public IndexReaderMetadataResolver getMetadataResolver() {
		return metadataResolver;
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
			closer.pushAll( DirectoryReaderHolder::close, directoryReaderHolders );
		}
		if ( debugEnabled ) {
			log.trace( "MultiReader closed." );
		}
	}

	private static IndexReader[] toReaderArray(List<DirectoryReaderHolder> directoryReaderHolders) {
		IndexReader[] indexReaders = new IndexReader[directoryReaderHolders.size()];
		for ( int i = 0; i < directoryReaderHolders.size(); i++ ) {
			indexReaders[i] = directoryReaderHolders.get( i ).get();
		}
		return indexReaders;
	}

	public static class Builder implements DirectoryReaderCollector {
		private final List<DirectoryReaderHolder> directoryReaderHolders = new ArrayList<>();
		private final Map<DirectoryReader, String> mappedTypeNameByDirectoryReader = new HashMap<>();

		private Builder() {
		}

		@Override
		public void collect(String mappedTypeName, DirectoryReaderHolder directoryReaderHolder) {
			directoryReaderHolders.add( directoryReaderHolder );
			DirectoryReader reader = directoryReaderHolder.get();
			mappedTypeNameByDirectoryReader.put( reader, mappedTypeName );
		}

		HolderMultiReader build() throws IOException {
			IndexReaderMetadataResolver metadataResolver =
					new IndexReaderMetadataResolver( mappedTypeNameByDirectoryReader );
			return new HolderMultiReader( directoryReaderHolders, metadataResolver );
		}
	}
}
