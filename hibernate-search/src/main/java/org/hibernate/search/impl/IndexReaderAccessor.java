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
package org.hibernate.search.impl;

import java.util.HashMap;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.indexes.ReaderAccessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.reader.impl.MultiReaderFactory;

/**
 * Provides access to IndexReaders.
 * IndexReaders opened through this service need to be closed using this service.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class IndexReaderAccessor implements ReaderAccessor {

	private final ImmutableSearchFactory searchFactory;

	public IndexReaderAccessor(ImmutableSearchFactory immutableSearchFactory) {
		this.searchFactory = immutableSearchFactory;
	}

	@Override
	public void closeIndexReader(IndexReader indexReader) {
		MultiReaderFactory.closeReader( indexReader );
	}

	@Override
	public IndexReader openIndexReader(Class<?>... entities) {
		HashMap<String, IndexManager> indexManagers = new HashMap<String, IndexManager>();
		for ( Class<?> type : entities ) {
			EntityIndexBinder entityIndexBinding = searchFactory.getSafeIndexBindingForEntity( type );
			IndexManager[] indexManagersForAllShards = entityIndexBinding.getSelectionStrategy()
					.getIndexManagersForAllShards();
			for ( IndexManager im : indexManagersForAllShards ) {
				indexManagers.put( im.getIndexName(), im );
			}
		}
		IndexManager[] uniqueIndexManagers = indexManagers.values().toArray( new IndexManager[indexManagers.size()] );
		return MultiReaderFactory.openReader( uniqueIndexManagers );
	}

}
