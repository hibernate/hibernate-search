/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.util.Arrays;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
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

	final IndexReader[] subReaders;
	final ReaderProvider[] managers;

	public ManagedMultiReader(IndexReader[] subReaders, ReaderProvider[] managers) {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( subReaders, true );
		this.subReaders = subReaders;
		this.managers = managers;
		assert subReaders.length == managers.length;
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
		for ( int i = 0; i < subReaders.length; i++ ) {
			ReaderProvider container = managers[i];
			container.closeIndexReader( subReaders[i] ); // might be virtual
		}
		if ( debugEnabled ) {
			log.trace( "MultiReader closed." );
		}
	}

	@Override
	public String toString() {
		return "CacheableMultiReader [subReaders=" + Arrays.toString( subReaders ) + ", managers=" + Arrays.toString( managers ) + "]";
	}

}
