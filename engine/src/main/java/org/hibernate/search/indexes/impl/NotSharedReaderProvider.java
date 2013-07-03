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

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Open a reader each time
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class NotSharedReaderProvider implements DirectoryBasedReaderProvider {

	private static final Log log = LoggerFactory.make();

	private DirectoryProvider directoryProvider;
	private String indexName;

	@Override
	public IndexReader openIndexReader() {
		// #getDirectory must be invoked each time as the underlying directory might "dance" as in
		// org.hibernate.search.store.impl.FSSlaveDirectoryProvider
		Directory directory = directoryProvider.getDirectory();
		try {
			return IndexReader.open( directory );
		}
		catch (IOException e) {
			throw new SearchException( "Could not open index \"" + indexName + "\"", e );
		}
	}

	@Override
	public void closeIndexReader(IndexReader reader) {
		try {
			reader.close();
		}
		catch (IOException e) {
			log.unableToCloseLuceneIndexReader( e );
		}
	}

	@Override
	public void initialize(DirectoryBasedIndexManager indexManager, Properties props) {
		directoryProvider = indexManager.getDirectoryProvider();
		indexName = indexManager.getIndexName();
	}

	@Override
	public void stop() {
		//nothing to do for this implementation
	}

}
