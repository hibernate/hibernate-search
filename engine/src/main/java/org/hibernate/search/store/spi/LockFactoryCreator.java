/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.spi;

import java.io.File;
import java.util.Properties;

import org.apache.lucene.store.LockFactory;
import org.hibernate.search.engine.service.spi.Service;

/**
 * A service for obtaining {@link LockFactory}s based on the current configuration.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public interface LockFactoryCreator extends Service {

	/**
	 * Creates a {@link LockFactory} as selected in the configuration for the Directory provider. The "simple" and
	 * "native" strategies a {@link File} to know where to store the file system based locks; other implementations may
	 * ignore this parameter.
	 *
	 * @param indexDir the directory to use to store locks, if needed by implementation
	 * @param dirConfiguration the configuration of current DirectoryProvider
	 * @return the lock factory as configured, or a factory adhering to the "simple" strategy in case of configuration
	 * errors or as a default.
	 */
	LockFactory createLockFactory(File indexDir, Properties dirConfiguration);
}
