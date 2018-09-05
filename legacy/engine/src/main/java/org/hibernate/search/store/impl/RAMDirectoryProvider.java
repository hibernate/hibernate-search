/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.impl;

import java.util.Properties;

import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.spi.DirectoryHelper;
import org.hibernate.search.store.spi.LockFactoryCreator;

/**
 * Use a Lucene RAMDirectory
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 */
public class RAMDirectoryProvider implements DirectoryProvider<RAMDirectory> {

	private RAMDirectory directory;

	private String indexName;
	private Properties properties;
	private ServiceManager serviceManager;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		this.indexName = directoryProviderName;
		this.properties = properties;
		this.serviceManager = context.getServiceManager();
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		try {
			LockFactory lockFactory = serviceManager
					.requestService( LockFactoryCreator.class )
					.createLockFactory( null, properties );
			directory = makeRAMDirectory( lockFactory );
			properties = null;
			DirectoryHelper.initializeIndexIfNeeded( directory );
		}
		finally {
			serviceManager.releaseService( LockFactoryCreator.class );
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
	 * @param lockFactory provides the locking strategy
	 * @return the RAMDirectory this provider is going to manage
	 */
	protected RAMDirectory makeRAMDirectory(LockFactory lockFactory) {
		return new RAMDirectory( lockFactory );
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
