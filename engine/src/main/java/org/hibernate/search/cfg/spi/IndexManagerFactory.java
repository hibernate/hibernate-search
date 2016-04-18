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
 * @author Guillaume Smet
 */
public interface IndexManagerFactory extends Service {

	/**
	 * Determine the {@code IndexManager} implementation which will be used for this entity type.
	 * @param indexManagerImplementationName how this is resolved to an {@code IndexManager} type
	 * is left to the implementor.
	 *
	 * @return the type chosen
	 */
	Class<? extends IndexManager> determineIndexManagerType(String indexManagerImplementationName);

	/**
	 * @param indexManagerType a given {@code IndexManager} type
	 * @return a new {@code IndexManager} instance of the given type
	 */
	IndexManager createIndexManager(Class<? extends IndexManager> indexManagerType);

}
