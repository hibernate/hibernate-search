/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.impl;

import java.io.IOException;
import java.io.InputStream;

import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;

/**
 * The Infinispan configuration is ClassLoader sensitive, this wrapper around
 * the standard Parser is used to allow it to find resources in a modular
 * classloading environment.
 *
 * @author Sanne Grinovero
 * @since 4.3
 */
public class InfinispanConfigurationParser {

	private final ParserRegistry configurationParser;
	private final ClassLoader ispnClassLoadr;

	public InfinispanConfigurationParser() {
		ispnClassLoadr = ParserRegistry.class.getClassLoader();
		configurationParser = new ParserRegistry( ispnClassLoadr );
	}

	/**
	 * Resolves an Infinispan configuration file but using the Hibernate Search
	 * classloader. The returned Infinispan configuration template also overrides
	 * Infinispan's runtime classloader to the one of Hibernate Search.
	 *
	 * @param filename Infinispan configuration resource name
	 * @param transportOverrideResource An alternative JGroups configuration file to be injected
	 * @param serviceManager the ServiceManager to load resources
	 * @throws IOException
	 * @return
	 */
	public ConfigurationBuilderHolder parseFile(String filename, String transportOverrideResource, ServiceManager serviceManager) throws IOException {
		ClassLoaderService classLoaderService = serviceManager.requestService( ClassLoaderService.class );
		try {
			return parseFile( classLoaderService, filename, transportOverrideResource );
		}
		finally {
			serviceManager.releaseService( ClassLoaderService.class );
		}
	}

	private ConfigurationBuilderHolder parseFile(ClassLoaderService classLoaderService, String filename, String transportOverrideResource) {
		InputStream is = classLoaderService.locateResourceStream( filename );
		try {
			ConfigurationBuilderHolder builderHolder = configurationParser.parse( is );
			//Workaround Infinispan's ClassLoader strategies to bend to our will:
			fixClassLoaders( builderHolder );
			patchTransportConfiguration( builderHolder, transportOverrideResource );
			return builderHolder;
		}
		finally {
			Util.close( is );
		}
	}

	private void fixClassLoaders(ConfigurationBuilderHolder builderHolder) {
		//Global section:
		builderHolder.getGlobalConfigurationBuilder().classLoader( ispnClassLoadr );
	}

	/**
	 * After having parsed the Infinispan configuration file, we might want to override the specified
	 * JGroups configuration file.
	 *
	 * @param builderHolder
	 * @param transportOverrideResource The alternative JGroups configuration file to be used, or null
	 */
	private void patchTransportConfiguration(ConfigurationBuilderHolder builderHolder, String transportOverrideResource) {
		if ( transportOverrideResource != null ) {
			builderHolder.getGlobalConfigurationBuilder().transport().addProperty( "configurationFile", transportOverrideResource );
		}
	}

}
