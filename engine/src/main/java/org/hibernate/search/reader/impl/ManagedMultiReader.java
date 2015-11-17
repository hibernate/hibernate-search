/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.reader.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.uninverting.UninvertingReader;
import org.apache.lucene.uninverting.UninvertingReader.Type;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.query.engine.impl.SortConfigurations;
import org.hibernate.search.query.engine.impl.SortConfigurations.SortConfiguration;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Wraps a MultiReader to keep references to owning managers.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class ManagedMultiReader extends MultiReader {

	private static final Log log = LoggerFactory.make();

	/**
	 * The index readers to be closed in {@link #doClose()}. Will be the readers originally given upon instantiation in
	 * case it was required to wrap those with {@link UninvertingReader}s for the purposes of sorting prior to passing
	 * them to super.
	 */
	final IndexReader[] readersForClosing;
	final ReaderProvider[] readerProviders;


	private ManagedMultiReader(IndexReader[] subReaders, IndexReader[] readersForClosing, ReaderProvider[] readerProviders) throws IOException {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( subReaders, true );
		assert readersForClosing.length == readerProviders.length;

		this.readersForClosing = readersForClosing;
		this.readerProviders = readerProviders;
	}

	static ManagedMultiReader createInstance(IndexManager[] indexManagers, SortConfigurations configuredSorts, Sort sort, boolean indexUninvertingAllowed) throws IOException {
		final int length = indexManagers.length;

		IndexReader[] subReaders = new IndexReader[length];
		ReaderProvider[] readerProviders = new ReaderProvider[length];
		for ( int index = 0; index < length; index++ ) {
			ReaderProvider indexReaderManager = indexManagers[index].getReaderProvider();
			IndexReader openIndexReader = indexReaderManager.openIndexReader();
			subReaders[index] = openIndexReader;
			readerProviders[index] = indexReaderManager;
		}

		IndexReader[] effectiveReaders = getEffectiveReaders( indexManagers, subReaders, configuredSorts, sort, indexUninvertingAllowed );
		return new ManagedMultiReader( effectiveReaders, subReaders, readerProviders );
	}

	/**
	 * Gets the readers to be effectively used. A given reader will be returned itself if:
	 * <ul>
	 * <li>there is no sort involved.</li>
	 * <li>doc value fields for all requested sort fields are contained in the index, for each entity type mapped to the
	 * index</li>
	 * </ul>
	 * Otherwise the directory reader will be wrapped in a {@link UninvertingReader} configured in a way to satisfy the
	 * requested sorts.
	 */
	private static IndexReader[] getEffectiveReaders(IndexManager[] indexManagers, IndexReader[] subReaders, SortConfigurations configuredSorts, Sort sort, boolean indexUninvertingAllowed) {
		if ( sort == null || sort.getSort().length == 0 ) {
			return subReaders;
		}

		Set<String> indexesToBeUninverted = getIndexesToBeUninverted( configuredSorts, sort, indexUninvertingAllowed );
		Map<String, Type> mappings = indexesToBeUninverted.isEmpty() ? Collections.<String, Type>emptyMap() : getMappings( sort );
		IndexReader[] effectiveReaders = new IndexReader[subReaders.length];

		int i = 0;
		for ( IndexReader reader : subReaders ) {
			// take incoming reader as is
			if ( !indexesToBeUninverted.contains( indexManagers[i].getIndexName() ) ) {
				effectiveReaders[i] = reader;
			}
			// wrap with uninverting reader
			else {
				if ( reader instanceof DirectoryReader ) {
					DirectoryReader directoryReader = (DirectoryReader) reader;

					try {
						effectiveReaders[i] = UninvertingReader.wrap( directoryReader, mappings );
					}
					catch (IOException e) {
						throw log.couldNotCreateUninvertingReader( directoryReader, e );
					}
				}
				else {
					log.readerTypeUnsupportedForInverting( reader.getClass() );
					effectiveReaders[i] = reader;
				}
			}

			i++;
		}

		return effectiveReaders;
	}

	/**
	 * Checks for each involved entity type whether it maps all the required sortable fields; If not, it marks the index
	 * for uninverting.
	 */
	private static Set<String> getIndexesToBeUninverted(SortConfigurations configuredSorts, Sort sort, boolean indexUninvertingAllowed) {
		Set<String> indexesToBeUninverted = new HashSet<>();

		for ( SortConfiguration sortConfiguration : configuredSorts ) {
			boolean foundEntityWithAllRequiredSorts = false;
			boolean foundEntityWithMissingSorts = false;

			for ( Class<?> entityType : sortConfiguration.getEntityTypes() ) {
				List<String> uncoveredSorts = sortConfiguration.getUncoveredSorts( entityType, sort );

				if ( !uncoveredSorts.isEmpty() ) {
					indexesToBeUninverted.add( sortConfiguration.getIndexName() );

					if ( indexUninvertingAllowed ) {
						log.uncoveredSortsRequested( entityType, sortConfiguration.getIndexName(), StringHelper.join( uncoveredSorts, ", " ) );
					}
					else {
						throw log.uncoveredSortsRequestedWithUninvertingNotAllowed(
								entityType,
								sortConfiguration.getIndexName(),
								StringHelper.join( uncoveredSorts, ", " )
						);
					}

					foundEntityWithMissingSorts = true;
				}
				else {
					foundEntityWithAllRequiredSorts = true;
				}
			}

			if ( foundEntityWithAllRequiredSorts && foundEntityWithMissingSorts ) {
				throw log.inconsistentSortableFieldConfigurationForSharedIndex( sortConfiguration.getIndexName(), StringHelper.join( sort.getSort(), ", " ) );
			}
		}

		return indexesToBeUninverted;
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
			ReaderProvider container = readerProviders[i];
			container.closeIndexReader( readersForClosing[i] ); // might be virtual
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
		return ManagedMultiReader.class.getSimpleName() + " [readersForClosing=" + Arrays.toString( readersForClosing ) + ", readerProviders=" + Arrays.toString( readerProviders ) + "]";
	}
}
