/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.util.HashMap;
import java.util.TreeSet;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.engine.impl.ImmutableSearchFactory;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Provides access to IndexReaders.
 * IndexReaders opened through this service need to be closed using this service.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class DefaultIndexReaderAccessor implements IndexReaderAccessor {

	private static final Log log = LoggerFactory.make();

	private final ImmutableSearchFactory searchFactory;

	public DefaultIndexReaderAccessor(ImmutableSearchFactory immutableSearchFactory) {
		this.searchFactory = immutableSearchFactory;
	}

	@Override
	public void close(IndexReader indexReader) {
		MultiReaderFactory.closeReader( indexReader );
	}

	@Override
	public IndexReader open(Class<?>... entities) {
		if ( entities.length == 0 ) {
			throw log.needAtLeastOneIndexedEntityType();
		}

		HashMap<String, IndexManager> indexManagers = new HashMap<String, IndexManager>();
		for ( Class<?> type : entities ) {
			EntityIndexBinding entityIndexBinding = searchFactory.getSafeIndexBindingForEntity( type );
			IndexManager[] indexManagersForAllShards = entityIndexBinding.getSelectionStrategy()
					.getIndexManagersForAllShards();
			for ( IndexManager im : indexManagersForAllShards ) {
				indexManagers.put( im.getIndexName(), im );
			}
		}
		IndexManager[] uniqueIndexManagers = indexManagers.values().toArray( new IndexManager[indexManagers.size()] );
		return MultiReaderFactory.openReader( uniqueIndexManagers );
	}

	@Override
	public IndexReader open(String... indexNames) {
		TreeSet<String> names = new TreeSet<String>();
		for ( String name : indexNames ) {
			if ( name != null ) {
				names.add( name );
			}
		}
		final int size = names.size();
		if ( size == 0 ) {
			throw log.needAtLeastOneIndexName();
		}
		String[] indexManagerNames = names.toArray( new String[size] );
		IndexManagerHolder managerSource = searchFactory.getIndexManagerHolder();
		IndexManager[] managers = new IndexManager[size];
		for ( int i = 0; i < size; i++ ) {
			String indexName = indexManagerNames[i];
			managers[i] = managerSource.getIndexManager( indexName );
			if ( managers[i] == null ) {
				throw log.requestedIndexNotDefined( indexName );
			}
		}
		return MultiReaderFactory.openReader( managers );
	}

}
