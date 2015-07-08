/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.impl;

import java.io.File;
import java.util.Properties;

import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.LockFactoryProvider;
import org.hibernate.search.store.spi.LockFactoryCreator;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The one and only {@link LockFactoryCreator}.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public class DefaultLockFactoryCreator implements LockFactoryCreator, Startable {

	private static final Log LOG = LoggerFactory.make();

	private ServiceManager serviceManager;

	@Override
	public void start(Properties properties, BuildContext context) {
		this.serviceManager = context.getServiceManager();
	}

	@Override
	public LockFactory createLockFactory(File indexDir, Properties dirConfiguration) {
		//For FS-based indexes default to "native", default to "single" otherwise.
		String defaultStrategy = indexDir == null ? "single" : "native";
		String lockFactoryName = dirConfiguration.getProperty( Environment.LOCKING_STRATEGY, defaultStrategy );
		if ( "simple".equals( lockFactoryName ) ) {
			if ( indexDir == null ) {
				throw LOG.indexBasePathRequiredForLockingStrategy( "simple" );
			}
			return SimpleFSLockFactory.INSTANCE;
		}
		else if ( "native".equals( lockFactoryName ) ) {
			if ( indexDir == null ) {
				throw LOG.indexBasePathRequiredForLockingStrategy( "native" );
			}
			return NativeFSLockFactory.INSTANCE;
		}
		else if ( "single".equals( lockFactoryName ) ) {
			return new SingleInstanceLockFactory();
		}
		else if ( "none".equals( lockFactoryName ) ) {
			return NoLockFactory.INSTANCE;
		}
		else {
			LockFactoryProvider lockFactoryFactory = ClassLoaderHelper.instanceFromName(
					LockFactoryProvider.class,
					lockFactoryName,
					Environment.LOCKING_STRATEGY,
					serviceManager
			);
			return lockFactoryFactory.createLockFactory( indexDir, dirConfiguration );
		}
	}
}
