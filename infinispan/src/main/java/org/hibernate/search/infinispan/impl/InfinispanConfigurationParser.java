/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.infinispan.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.infinispan.commons.util.FileLookup;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
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
	private final ClassLoader searchConfigClassloader;
	private final ClassLoader userDeploymentClassloader;

	public InfinispanConfigurationParser(ClassLoader searchConfigClassloader) {
		this.searchConfigClassloader = searchConfigClassloader;
		//The parser itself loads extensions from the Infinispan modules so
		//needs to be pointed to the Infinispan module.
		ClassLoader ispnClassLoadr = ParserRegistry.class.getClassLoader();
		configurationParser = new ParserRegistry( ispnClassLoadr );
		this.userDeploymentClassloader = Thread.currentThread().getContextClassLoader();
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
		FileLookup fileLookup = FileLookupFactory.newInstance();
		InputStream is = fileLookup.lookupFile( filename, searchConfigClassloader );
		if ( is == null ) {
			is = fileLookup.lookupFile( filename, userDeploymentClassloader );
			if ( is == null ) {
				throw new FileNotFoundException( filename );
			}
		}
		try {
			ConfigurationBuilderHolder builderHolder = configurationParser.parse( is );
			patchTransportConfiguration( builderHolder, transportOverrideResource );
			patchInfinispanClassLoader( builderHolder );
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

	/**
	 * Changes the state of the passed configuration Builder to apply specific classloader needs
	 */
	private void patchInfinispanClassLoader(ConfigurationBuilderHolder configurationBuilderHolder) {
		configurationBuilderHolder.getGlobalConfigurationBuilder().classLoader( searchConfigClassloader );
		configurationBuilderHolder.getDefaultConfigurationBuilder().classLoader( searchConfigClassloader );
		for ( ConfigurationBuilder cfg : configurationBuilderHolder.getNamedConfigurationBuilders().values() ) {
			cfg.classLoader( searchConfigClassloader );
		}
	}

}
