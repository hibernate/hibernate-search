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
package org.hibernate.search.store.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.search.SearchException;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * Create a Lucene directory provider which can be configured
 * through the following properties:
 * <ul>
 * <li><i>hibernate.search.default.*</i></li>
 * <li><i>hibernate.search.&lt;indexname&gt;.*</i>,</li>
 * </ul>where <i>&lt;indexname&gt;</i> properties have precedence over default ones.
 * <p/>
 * The implementation is described by
 * <i>hibernate.search.[default|indexname].directory_provider</i>.
 * If none is defined the default value is FSDirectory.
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class DirectoryProviderFactory {

	private static final Map<String, String> defaultProviderClasses;

	static {
		defaultProviderClasses = new HashMap<String, String>( 6 );
		defaultProviderClasses.put( "", FSDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem", FSDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem-master", FSMasterDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem-slave", FSSlaveDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "ram", RAMDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "infinispan", "org.hibernate.search.infinispan.impl.InfinispanDirectoryProvider" );
	}

	public static DirectoryProvider<?> createDirectoryProvider(String directoryProviderName, Properties indexProps, WorkerBuildContext context) {
		String className = indexProps.getProperty( "directory_provider", "" );
		String maybeShortCut = className.toLowerCase();

		DirectoryProvider<?> provider;
		//try and use the built-in shortcuts before loading the provider as a fully qualified class name 
		if ( defaultProviderClasses.containsKey( maybeShortCut ) ) {
			String fullClassName = defaultProviderClasses.get( maybeShortCut );
			provider = ClassLoaderHelper.instanceFromName( DirectoryProvider.class,
					fullClassName, DirectoryProviderFactory.class, "directory provider" );
		}
		else {
			provider = ClassLoaderHelper.instanceFromName(
					DirectoryProvider.class, className,
					DirectoryProviderFactory.class, "directory provider"
			);
		}
		try {
			provider.initialize( directoryProviderName, indexProps, context );
		}
		catch ( Exception e ) {
			throw new SearchException( "Unable to initialize directory provider: " + directoryProviderName, e );
		}
		return provider;
	}

}
