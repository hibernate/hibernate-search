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
package org.hibernate.search.reader;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.SearchException;

import static org.hibernate.search.reader.ReaderProviderHelper.buildMultiReader;
import static org.hibernate.search.reader.ReaderProviderHelper.clean;

import org.hibernate.search.store.DirectoryProvider;

/**
 * Open a reader each time
 *
 * @author Emmanuel Bernard
 */
public class NotSharedReaderProvider implements ReaderProvider {
	@SuppressWarnings( { "ThrowableInstanceNeverThrown" } )
	public IndexReader openReader(DirectoryProvider... directoryProviders) {
		final int length = directoryProviders.length;
		IndexReader[] readers = new IndexReader[length];
		try {
			for (int index = 0; index < length; index++) {
				readers[index] = IndexReader.open( directoryProviders[index].getDirectory(), true );
			}
		}
		catch (IOException e) {
			//TODO more contextual info
			clean( new SearchException( "Unable to open one of the Lucene indexes", e ), readers );
		}
		return buildMultiReader( length, readers );
	}


	@SuppressWarnings( { "ThrowableInstanceNeverThrown" } )
	public void closeReader(IndexReader reader) {
		try {
			reader.close();
		}
		catch (IOException e) {
			//TODO extract subReaders and close each one individually
			clean( new SearchException( "Unable to close multiReader" ), reader );
		}
	}

	public void initialize(Properties props, BuildContext context) {
	}

	public void destroy() {
	}
}
