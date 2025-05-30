/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.LuceneMiscLog;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

/**
 * A {@link MultiReader} keeping references to {@link DirectoryReader}s to eventually close them,
 * and holding some additional metadata related to the targeted readers.
 * <p>
 * Ported from Search 5: {@code org.hibernate.search.reader.impl.ManagedMultiReader},
 * {@code org.hibernate.search.reader.impl.MultiReaderFactory}.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class HibernateSearchMultiReader extends MultiReader {

	public static HibernateSearchMultiReader open(Set<String> indexNames,
			Collection<? extends ReadIndexManagerContext> indexManagerContexts, Set<String> routingKeys) {
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
						.pushAll( builder.directoryReaders );
				throw LuceneMiscLog.INSTANCE.unableToOpenIndexReaders(
						e.getMessage(), EventContexts.fromIndexNames( indexNames ), e
				);
			}
		}
	}

	private final List<DirectoryReader> directoryReaders;
	private final IndexReaderMetadataResolver metadataResolver;

	HibernateSearchMultiReader(List<DirectoryReader> directoryReaders, IndexReaderMetadataResolver metadataResolver)
			throws IOException {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( toReaderArray( directoryReaders ), true );
		this.directoryReaders = directoryReaders;
		this.metadataResolver = metadataResolver;
	}

	public IndexReaderMetadataResolver getMetadataResolver() {
		return metadataResolver;
	}

	@Override
	protected synchronized void doClose() throws IOException {
		final boolean traceEnabled = LuceneMiscLog.INSTANCE.isTraceEnabled();
		if ( traceEnabled ) {
			LuceneMiscLog.INSTANCE.closingMultiReader( this );
		}
		try ( Closer<IOException> closer = new Closer<>() ) {
			/*
			 * Important: we decrement a usage counter instead of directly closing the reader,
			 * just in case the reader is shared.
			 * If the reader is not shared, this is equivalent to closing the reader.
			 */
			closer.pushAll( DirectoryReader::decRef, directoryReaders );
		}
		if ( traceEnabled ) {
			LuceneMiscLog.INSTANCE.closedMultiReader( this );
		}
	}

	private static IndexReader[] toReaderArray(List<DirectoryReader> directoryReaders) {
		return directoryReaders.toArray( new DirectoryReader[0] );
	}

	public static class Builder implements DirectoryReaderCollector {
		private final List<DirectoryReader> directoryReaders = new ArrayList<>();
		private final Map<DirectoryReader, String> mappedTypeNameByDirectoryReader = new HashMap<>();

		private Builder() {
		}

		@Override
		public void collect(String mappedTypeName, DirectoryReader directoryReader) {
			directoryReaders.add( directoryReader );
			mappedTypeNameByDirectoryReader.put( directoryReader, mappedTypeName );
		}

		HibernateSearchMultiReader build() throws IOException {
			IndexReaderMetadataResolver metadataResolver =
					new IndexReaderMetadataResolver( mappedTypeNameByDirectoryReader );
			return new HibernateSearchMultiReader( directoryReaders, metadataResolver );
		}
	}
}
