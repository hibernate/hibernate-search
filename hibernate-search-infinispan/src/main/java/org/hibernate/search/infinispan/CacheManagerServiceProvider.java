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
package org.hibernate.search.infinispan;

import java.io.IOException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.JGroupsHelper;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.util.NamingHelper;
import org.hibernate.util.PropertiesHelper;

/**
 * Provides access to Infinispan's CacheManager; one CacheManager is needed for all caches,
 * it can be taken via JNDI or started by this ServiceProvider; in this case it will also
 * be stopped when no longer needed.
 *
 * @author Sanne Grinovero
 */
public class CacheManagerServiceProvider implements ServiceProvider<EmbeddedCacheManager> {

	private static final Logger log = LoggerFactory.make();

	/**
	 * If no configuration is defined an no JNDI lookup name is provided, than a new Infinispan CacheManager
	 * will be started using this configuration. Such a configuration file is provided in Hibernate Search's
	 * jar.
	 */
	public static final String DEFAULT_INFINISPAN_CONFIGURATION_RESOURCENAME = "default-hibernatesearch-infinispan.xml";

	/**
	 * Reuses the same JNDI name from the second level cache implementation
	 * based on Infinispan
	 *
	 * @see org.hibernate.cache.infinispan.JndiInfinispanRegionFactory.CACHE_MANAGER_RESOURCE_PROP
	 */
	public static final String CACHE_MANAGER_RESOURCE_PROP = "hibernate.search.infinispan.cachemanager_jndiname";

	/**
	 * The configuration property to use as key to define a custom configuration for Infinispan.
	 * Ignored if hibernate.search.infinispan.cachemanager_jndiname is defined.
	 */
	public static final String INFINISPAN_CONFIGURATION_RESOURCENAME = "hibernate.search.infinispan.configuration_resourcename";

	private EmbeddedCacheManager cacheManager;

	/**
	 * JNDI retrieved cachemanagers are not started by us, so avoid attempting
	 * to close them.
	 */
	private boolean manageCacheManager = false;

	@Override
	public void start(Properties properties) {
		JGroupsHelper.verifyIPv4IsPreferred();
		String name = PropertiesHelper.getString( CACHE_MANAGER_RESOURCE_PROP, properties, null );
		if ( name == null ) {
			// No JNDI lookup configured: start the CacheManager
			String cfgName = properties.getProperty(
					INFINISPAN_CONFIGURATION_RESOURCENAME,
					DEFAULT_INFINISPAN_CONFIGURATION_RESOURCENAME
			);
			try {
				cacheManager = new DefaultCacheManager( cfgName );
				cacheManager.start();
				manageCacheManager = true;
			}
			catch ( IOException e ) {
				throw new SearchException(
						"Could not start Infinispan CacheManager using as configuration file: " + cfgName, e
				);
			}
		}
		else {
			// use the CacheManager via JNDI
			cacheManager = locateCacheManager( name, NamingHelper.getJndiProperties( properties ) );
			manageCacheManager = false;
		}
	}

	private EmbeddedCacheManager locateCacheManager(String jndiNamespace, Properties jndiProperties) {
		Context ctx = null;
		try {
			ctx = new InitialContext( jndiProperties );
			return (EmbeddedCacheManager) ctx.lookup( jndiNamespace );
		}
		catch ( NamingException ne ) {
			String msg = "Unable to retrieve CacheManager from JNDI [" + jndiNamespace + "]";
			log.error( msg, ne );
			throw new SearchException( msg );
		}
		finally {
			if ( ctx != null ) {
				try {
					ctx.close();
				}
				catch ( NamingException ne ) {
					log.error( "Unable to release initial context", ne );
				}
			}
		}
	}

	@Override
	public EmbeddedCacheManager getService() {
		return cacheManager;
	}

	@Override
	public void stop() {
		if ( cacheManager != null && manageCacheManager ) {
			cacheManager.stop();
		}
	}
}
