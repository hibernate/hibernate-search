/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Properties;

import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.indexes.impl.NRTIndexManager;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This is the default {@code IndexManager} implementation for Hibernate Search.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public class DefaultIndexManagerFactory implements IndexManagerFactory, Startable {

	private static final Log log = LoggerFactory.make();

	private ServiceManager serviceManager;

	@Override
	public IndexManager createDefaultIndexManager() {
		return new DirectoryBasedIndexManager();
	}

	@Override
	public IndexManager createIndexManagerByName(String indexManagerImplementationName) {
		if ( StringHelper.isEmpty( indexManagerImplementationName ) ) {
			return createDefaultIndexManager();
		}
		else {
			indexManagerImplementationName = indexManagerImplementationName.trim();
			IndexManager indexManager = fromAlias( indexManagerImplementationName );
			if ( indexManager == null ) {
				indexManagerImplementationName = aliasToFQN( indexManagerImplementationName );
				Class<?> indexManagerClass = ClassLoaderHelper.classForName(
						indexManagerImplementationName,
						serviceManager
				);
				indexManager = ClassLoaderHelper.instanceFromClass(
						IndexManager.class,
						indexManagerClass,
						"index manager"
				);
			}
			log.indexManagerAliasResolved( indexManagerImplementationName, indexManager.getClass() );
			return indexManager;
		}
	}

	@Override
	public void start(Properties properties, BuildContext context) {
		this.serviceManager = context.getServiceManager();
	}

	/**
	 * Provide a way to expand known aliases to fully qualified class names.
	 * As opposed to {@link #fromAlias(String)} we can use this to expand to well
	 * known implementations which are optional on the classpath.
	 *
	 * @param alias the alias to replace with the fully qualified class name of the implementation
	 *
	 * @return the same name, or a fully qualified class name to use instead
	 */
	protected String aliasToFQN(final String alias) {
		// TODO Add the Infinispan implementor here
		return alias;
	}

	/**
	 * Extension point: allow to override aliases or add new ones to
	 * directly create class instances.
	 *
	 * @param alias the requested alias
	 *
	 * @return return the index manager for the given alias or {@code null} if the alias is unknown.
	 */
	protected IndexManager fromAlias(String alias) {
		if ( "directory-based".equals( alias ) ) {
			return new DirectoryBasedIndexManager();
		}
		if ( "near-real-time".equals( alias ) ) {
			return new NRTIndexManager();
		}
		return null;
	}
}
