/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.spi;

import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * By implementing this integration point you can customize the creation of IndexManager instances.
 * Example usage is to override {@code ClassLoader}s used to resolve implementation names,
 * define new short-hand aliases, change the default implementation.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public interface IndexManagerFactory extends Service {

	/**
	 * @return a new instance of the default IndexManager
	 */
	IndexManager createDefaultIndexManager();

	/**
	 * @param indexManagerImplementationName how this is resolved to an IndexManager type
	 * is left to the implementor.
	 *
	 * @return a new IndexManager instance of the chosen type
	 */
	IndexManager createIndexManagerByName(String indexManagerImplementationName);

}
