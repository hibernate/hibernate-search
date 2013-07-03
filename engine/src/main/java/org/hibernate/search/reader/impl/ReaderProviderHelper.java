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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public abstract class ReaderProviderHelper {

	private static final Log log = LoggerFactory.make();

	private static final Field subReadersField = getSubReadersField();

	private static Field getSubReadersField() {
		try {
			Field field = MultiReader.class.getDeclaredField( "subReaders" );
			ReflectionHelper.setAccessible( field );
			return field;
		}
		catch (NoSuchFieldException e) {
			throw new SearchException( "Incompatible version of Lucene: MultiReader.subReaders not available", e );
		}
	}

	public static IndexReader[] getSubReadersFromMultiReader(MultiReader parentReader) {
		try {
			return (IndexReader[]) subReadersField.get( parentReader );
		}
		catch (IllegalAccessException e) {
			throw new SearchException( "Incompatible version of Lucene: MultiReader.subReaders not accessible", e );
		}
	}

	public static IndexReader buildMultiReader(int length, IndexReader[] readers, ReaderProvider[] managers) {
		if ( length == 0 ) {
			return null;
		}
		else {
			//everything should be the same so wrap in an MultiReader
			return new CacheableMultiReader( readers, managers );
		}
	}

	public static void clean(SearchException e, IndexReader... readers) {
		for ( IndexReader reader : readers ) {
			if ( reader != null ) {
				try {
					reader.close();
				}
				catch (IOException ee) {
					log.unableToCloseLuceneIndexReader( e );
				}
			}
		}
		throw e;
	}

	/**
	 * Find the underlying IndexReaders for the given searchable
	 *
	 * @param searchable The searchable to find the IndexReaders for
	 * @return A list of all base IndexReaders used within this searchable
	 */
	public static Set<IndexReader> getIndexReaders(IndexSearcher searchable) {
		Set<IndexReader> readers = new HashSet<IndexReader>();
		getIndexReadersInternal( readers, searchable );
		return readers;
	}

	/**
	 * Find the underlying IndexReaders for the given reader
	 *
	 * @param reader The reader to find the IndexReaders for
	 * @return A list of all base IndexReaders used within this searchable
	 */
	public static Set<IndexReader> getIndexReaders(IndexReader reader) {
		Set<IndexReader> readers = new HashSet<IndexReader>();
		getIndexReadersInternal( readers, reader );
		return readers;
	}

	/**
	 * Recursive method should identify all underlying readers for any nested structure of Lucene Searchable or IndexReader
	 *
	 * @param readers The working list of all readers found
	 * @param obj	 The object to find the readers within
	 */
	private static void getIndexReadersInternal(Set<IndexReader> readers, Object obj) {
		if ( obj instanceof IndexSearcher ) {
			getIndexReadersInternal( readers, ( (IndexSearcher) obj ).getIndexReader() );
		}
		else if ( obj instanceof IndexReader ) {
			readers.add( (IndexReader) obj );
		}
	}

}
