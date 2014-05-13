/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.impl;

import java.io.IOException;
import java.io.InputStream;

import org.hibernate.search.util.impl.AggregatedClassLoader;
import org.infinispan.commons.util.FileLookup;
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
	private final ClassLoader compositeClassLoader;

	public InfinispanConfigurationParser(ClassLoader searchConfigClassloader) {
		//The parser itself loads extensions from the Infinispan modules so
		//needs to be pointed to the Infinispan module.
		final ClassLoader ispnClassLoadr = ParserRegistry.class.getClassLoader();
		final ClassLoader userDeploymentClassloader = Thread.currentThread().getContextClassLoader();
		compositeClassLoader = new AggregatedClassLoader( searchConfigClassloader, userDeploymentClassloader, ispnClassLoadr );
		configurationParser = new ParserRegistry( compositeClassLoader );
	}

	/**
	 * Resolves an Infinispan configuration file but using the Hibernate Search
	 * classloader. The returned Infinispan configuration template also overrides
	 * Infinispan's runtime classloader to the one of Hibernate Search.
	 *
	 * @param filename Infinispan configuration resource name
	 * @param transportOverrideResource An alternative JGroups configuration file to be injected
	 * @throws IOException
	 * @return
	 */
	public ConfigurationBuilderHolder parseFile(String filename, String transportOverrideResource) throws IOException {
		FileLookup fileLookup = new FileLookup();
		InputStream is = fileLookup.lookupFileStrict( filename, compositeClassLoader );
		try {
			ConfigurationBuilderHolder builderHolder = configurationParser.parse( is );
			patchTransportConfiguration( builderHolder, transportOverrideResource );
			return builderHolder;
		}
		finally {
			Util.close( is );
		}
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
