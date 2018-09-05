/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.spi;

import java.util.Properties;

import org.apache.lucene.store.Directory;

import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;

/**
 * This Service allows to customize the creation of {@link org.hibernate.search.store.DirectoryProvider}
 * instances, potentially making use of shortcuts for the implementation names and altering default implementations.
 *
 * @author gustavonalle
 * @param <D> a Lucene directory provider
 */
public interface DirectoryProviderService<D extends Directory> extends Service {

	/**
	 * Creates a DirectoryProvider for an index based on the configuration
	 *
	 * @param indexProperties the configuration properties
	 * @param indexName the name of the index (directory) to create
	 * @param context provide access to some services at initialization
	 * @return a {@link DirectoryProvider}
	 */
	DirectoryProvider<D> create(Properties indexProperties, String indexName, BuildContext context);

	/**
	 * Default {@link DirectoryProvider} to be used if none is configured by the user
	 *
	 * @return the default {@link DirectoryProvider}
	 */
	Class<? extends DirectoryProvider<D>> getDefault();

	/**
	 * Resolve short names into implementation names
	 *
	 * @param shortcut the short name of the directory provider
	 * @return the fully qualified class name of the directory provider identified by the shortcut
	 */
	String toFullyQualifiedClassName(String shortcut);

}
