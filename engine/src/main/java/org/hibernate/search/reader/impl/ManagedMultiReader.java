/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.reader.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.uninverting.UninvertingReader;
import org.apache.lucene.uninverting.UninvertingReader.Type;
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

	/**
	 * The index readers to be closed in {@link #doClose()}. Will be the readers originally given upon instantiation in
	 * case it was required to wrap those with {@link UninvertingReader}s for the purposes of sorting prior to passing
	 * them to super.
	 */
	final IndexReader[] readersForClosing;
	final ReaderProvider[] managers;

	private ManagedMultiReader(IndexReader[] subReaders, IndexReader[] readersForClosing, ReaderProvider[] managers) throws IOException {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( subReaders, true );

		this.readersForClosing = readersForClosing;
		this.managers = managers;
		assert readersForClosing.length == managers.length;
	}

	public static ManagedMultiReader createInstance(IndexReader[] subReaders, ReaderProvider[] managers, Sort sort) throws IOException {
		IndexReader[] effectiveReaders = getEffectiveReaders( subReaders, sort );
		return new ManagedMultiReader( effectiveReaders, subReaders, managers );
	}

	/**
	 * Gets the readers to be effectively used. Will be the given readers, if there is no sort involved. Otherwise each
	 * directory reader will be wrapped in a {@link UninvertingReader} configured in a way to satisfy the requested
	 * sorts.
	 */
	private static IndexReader[] getEffectiveReaders(IndexReader[] subReaders, Sort sort) {
		if ( sort == null || sort.getSort().length == 0 ) {
			return subReaders;
		}

		IndexReader[] uninvertingReaders = new IndexReader[subReaders.length];
		Map<String, Type> mappings = getMappings( sort );

		int i = 0;
		for ( IndexReader reader : subReaders ) {
			if ( reader instanceof DirectoryReader ) {
				DirectoryReader directoryReader = (DirectoryReader) reader;

				try {
					uninvertingReaders[i] = UninvertingReader.wrap( directoryReader, mappings );
				}
				catch (IOException e) {
					throw log.couldNotCreateUninvertingReader( directoryReader, e );
				}
			}
			else {
				log.readerTypeUnsupportedForInverting( reader.getClass() );
				uninvertingReaders[i] = reader;
			}

			i++;
		}

		return uninvertingReaders;
	}

	/**
	 * Returns the uninverting reader mappings required for the given non-null sort.
	 */
	private static Map<String, UninvertingReader.Type> getMappings(Sort sort) {
		Map<String,UninvertingReader.Type> mappings = new HashMap<>();

		for ( SortField sortField : sort.getSort() ) {
			if ( sortField.getField() != null ) {
				switch ( sortField.getType() ) {
					case INT: mappings.put( sortField.getField(), Type.INTEGER );
					break;
					case LONG: mappings.put( sortField.getField(), Type.LONG );
					break;
					case FLOAT: mappings.put( sortField.getField(), Type.FLOAT );
					break;
					case DOUBLE: mappings.put( sortField.getField(), Type.DOUBLE );
					break;
					case STRING:
					case STRING_VAL: mappings.put( sortField.getField(), Type.SORTED );
					break;
					case BYTES: mappings.put( sortField.getField(), Type.BINARY );
					break;
					case CUSTOM: // Nothing to do; expecting doc value fields created by the user
					break;
					default: log.sortFieldTypeUnsupported( sortField.getField(), sortField.getType() );
				}
			}
		}

		return mappings;
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
		for ( int i = 0; i < readersForClosing.length; i++ ) {
			ReaderProvider container = managers[i];
			container.closeIndexReader( readersForClosing[i] ); // might be virtual
		}
		if ( debugEnabled ) {
			log.trace( "MultiReader closed." );
		}
	}

	@Override
	public String toString() {
		return ManagedMultiReader.class.getSimpleName() + " [readersForClosing=" + Arrays.toString( readersForClosing ) + ", managers=" + Arrays.toString( managers ) + "]";
	}
}
