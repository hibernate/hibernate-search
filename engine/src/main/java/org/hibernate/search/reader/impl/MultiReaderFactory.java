/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.reader.impl;

import org.apache.lucene.index.IndexReader;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Creates and closes the IndexReaders encompassing multiple indexes.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public final class MultiReaderFactory {

	private static final Log log = LoggerFactory.make();

	private MultiReaderFactory() {
		//not allowed
	}

	public static IndexReader openReader(IndexManager... indexManagers) {
		final int length = indexManagers.length;
		IndexReader[] readers = new IndexReader[length];
		ReaderProvider[] managers = new ReaderProvider[length];
		for ( int index = 0; index < length; index++ ) {
			ReaderProvider indexReaderManager = indexManagers[index].getReaderProvider();
			IndexReader openIndexReader = indexReaderManager.openIndexReader();
			readers[index] = openIndexReader;
			managers[index] = indexReaderManager;
		}
		return ReaderProviderHelper.buildMultiReader( length, readers, managers );
	}

	public static void closeReader(IndexReader multiReader) {
		if ( multiReader == null ) {
			return;
		}
		IndexReader[] readers;
		ReaderProvider[] managers;
		if ( multiReader instanceof CacheableMultiReader ) {
			CacheableMultiReader castMultiReader = (CacheableMultiReader) multiReader;
			readers = ReaderProviderHelper.getSubReadersFromMultiReader( castMultiReader );
			managers = castMultiReader.managers;
		}
		else {
			throw new AssertionFailure( "Everything should be wrapped in a CacheableMultiReader" );
		}
		log.debugf( "Closing MultiReader: %s", multiReader );
		for ( int i = 0; i < readers.length; i++ ) {
			ReaderProvider container = managers[i];
			container.closeIndexReader( readers[i] ); // might be virtual
		}
		log.trace( "IndexReader closed." );
	}

}
