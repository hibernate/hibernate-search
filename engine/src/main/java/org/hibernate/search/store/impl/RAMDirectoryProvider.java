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
package org.hibernate.search.store.impl;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Use a Lucene RAMDirectory
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 */
public class RAMDirectoryProvider implements DirectoryProvider<RAMDirectory> {

	private final RAMDirectory directory = makeRAMDirectory();

	private String indexName;
	private Properties properties;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		indexName = directoryProviderName;
		this.properties = properties;
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		try {
			directory.setLockFactory( DirectoryProviderHelper.createLockFactory( null, properties ) );
			properties = null;
			DirectoryProviderHelper.initializeIndexIfNeeded( directory );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + indexName, e );
		}
	}


	@Override
	public RAMDirectory getDirectory() {
		return directory;
	}

	@Override
	public void stop() {
		directory.close();
	}

	/**
	 * To allow extensions to create different RAMDirectory flavours:
	 * @return the RAMDirectory this provider is going to manage
	 */
	protected RAMDirectory makeRAMDirectory() {
		return new RAMDirectory();
	}

	@Override
	public boolean equals(Object obj) {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		if ( obj == this ) {
			return true;
		}
		if ( obj == null || !( obj instanceof RAMDirectoryProvider ) ) {
			return false;
		}
		return indexName.equals( ( (RAMDirectoryProvider) obj ).indexName );
	}

	@Override
	public int hashCode() {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		int hash = 7;
		return 29 * hash + indexName.hashCode();
	}

}
