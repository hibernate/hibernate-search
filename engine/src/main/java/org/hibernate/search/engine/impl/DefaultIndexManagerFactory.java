/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Properties;

import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
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
 * This is the default {@code IndexManagerFactory} implementation for Hibernate Search.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @author Hardy Ferentschik
 * @author Guillaume Smet
 */
public class DefaultIndexManagerFactory implements IndexManagerFactory, Startable {

	private static final Log log = LoggerFactory.make();
	private static final String ES_INDEX_MANAGER = "org.hibernate.search.backend.elasticsearch.impl.ElasticsearchIndexManager";

	private ServiceManager serviceManager;

	@Override
	public void start(Properties properties, BuildContext context) {
		this.serviceManager = context.getServiceManager();
	}

	@Override
	public Class<? extends IndexManager> determineIndexManagerType(String indexManagerImplementationName) {
		Class<? extends IndexManager> indexManagerType;
		if ( StringHelper.isEmpty( indexManagerImplementationName ) ) {
			indexManagerType = getDefaultIndexManagerType();
		}
		else {
			indexManagerImplementationName = indexManagerImplementationName.trim();
			indexManagerType = fromAlias( indexManagerImplementationName );
			if ( indexManagerType == null ) {
				indexManagerImplementationName = aliasToFQN( indexManagerImplementationName );
				indexManagerType = ClassLoaderHelper.classForName(
						IndexManager.class,
						indexManagerImplementationName,
						"index manager",
						serviceManager
				);
			}
			log.indexManagerAliasResolved( indexManagerImplementationName, indexManagerType );
		}
		return indexManagerType;
	}

	@Override
	public IndexManager createIndexManager(Class<? extends IndexManager> indexManagerType) {
		IndexManager indexManager = ClassLoaderHelper.instanceFromClass(
				IndexManager.class,
				indexManagerType,
				"index manager"
		);

		return indexManager;
	}

	/**
	 * Returns the default {@code IndexManager} implementation to use.
	 *
	 * @return the default {@code IndexManager} type
	 */
	protected Class<? extends IndexManager> getDefaultIndexManagerType() {
		return DirectoryBasedIndexManager.class;
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
	 * @return return the index manager type for the given alias or {@code null} if the alias is unknown.
	 */
	protected Class<? extends IndexManager> fromAlias(String alias) {
		if ( "directory-based".equals( alias ) ) {
			return DirectoryBasedIndexManager.class;
		}
		if ( "near-real-time".equals( alias ) ) {
			return NRTIndexManager.class;
		}
		// TODO HSEARCH-2115 Remove once generic alias resolver contribution scheme is implemented
		else if ( "elasticsearch".equals( alias ) ) {
			try ( ServiceReference<ClassLoaderService> classLoaderService = serviceManager.requestReference( ClassLoaderService.class ) ) {
				Class<?> indexManagerType = classLoaderService.get().classForName( ES_INDEX_MANAGER );
				return (Class<? extends IndexManager>) indexManagerType;
			}
		}
		return null;
	}
}
