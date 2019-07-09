/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProviderInitializationContext;
import org.hibernate.search.util.common.impl.SuppressingCloser;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;

public class LocalHeapDirectoryProvider implements DirectoryProvider {

	public static final String NAME = "local-heap";

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void initialize(DirectoryProviderInitializationContext context) {
		// Nothing to do
	}

	@Override
	public DirectoryHolder createDirectory(DirectoryCreationContext context) throws IOException {
		// TODO HSEARCH-3440 re-allow configuring the lock factory
		//  see org.hibernate.search.store.impl.DefaultLockFactoryCreator.createLockFactory
		Directory directory = new ByteBuffersDirectory( new SingleInstanceLockFactory() );
		try {
			context.initializeIndexIfNeeded( directory );
			return DirectoryHolder.of( directory );
		}
		catch (IOException | RuntimeException e) {
			new SuppressingCloser( e ).push( directory );
			throw e;
		}
	}
}
