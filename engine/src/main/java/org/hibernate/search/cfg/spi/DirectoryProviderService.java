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
 */
public interface DirectoryProviderService<D extends Directory> extends Service {

	/**
	 * Creates a DirectoryProvider for an index based on the configuration
	 */
	DirectoryProvider<D> create(Properties indexProperties, String indexName, BuildContext context);

	/**
	 * Default DirectoryProvider to be used if none is configured by the user
	 */
	Class<? extends DirectoryProvider<D>> getDefault();

	/**
	 * Resolve short names into implementation names
	 */
	String toFullyQualifiedClassName(String shortcut);

}
