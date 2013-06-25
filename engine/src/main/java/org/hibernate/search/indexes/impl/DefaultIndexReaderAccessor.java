/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes.impl;

import java.util.HashMap;
import java.util.TreeSet;

import org.apache.lucene.index.IndexReader;

import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.impl.ImmutableSearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Provides access to IndexReaders.
 * IndexReaders opened through this service need to be closed using this service.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
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
