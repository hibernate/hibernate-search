/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store;

import java.io.File;
import java.util.Properties;

import org.apache.lucene.store.LockFactory;

/**
 * To use a custom implementation of {@link org.apache.lucene.store.LockFactory}
 * you need to implement this interface and define the fully qualified
 * classname of the factory implementation as a DirectoryProvider parameter
 * for the {@link org.hibernate.search.cfg.Environment#LOCKING_STRATEGY} key.
 * The implementation must have a no-arg constructor.
 *
 * @author Sanne Grinovero
 */
public interface LockFactoryProvider {

	/**
	 * Creates a LockFactory implementation.
	 * A different LockFactory is created for each DirectoryProvider.
	 *
	 * @param indexDir path to the indexBase setting, or null for
	 * DirectoryProviders which don't rely on filesystem
	 * @param dirConfiguration the properties set on the current DirectoryProvider
	 * @return the created LockFactory
	 */
	LockFactory createLockFactory(File indexDir, Properties dirConfiguration);

}
