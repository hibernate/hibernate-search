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
package org.hibernate.search.spi;

import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.impl.IndexManagerHolder;

/**
 * Build context that can be used by some services at initialization
 *
 * @author Emmanuel Bernard
 */
public interface BuildContext {
	/**
	 * Returns the SessionFactoryImplementor instance. Do not use until after the initialize and/or start method is
	 * fully executed.
	 * Implementations should not cache values provided by the SessionFactoryImplementor but rather access them
	 * each time: when the configuration is dynamically updated, new changes are available through the
	 * SearchFactoryImplementor
	 * For example, prefer
	 * <code>
	 * void method() {
	 *   int size = sfi.getDirectoryProviders().size();
	 * }
	 * </code>
	 * to
	 * <code>
	 * void method() {
	 *   int size = directoryProviders.size();
	 * }
	 * </code>
	 * where directoryProviders is a class variable.
	 */
	SearchFactoryImplementor getUninitializedSearchFactory();

	String getIndexingStrategy();

	/**
	 * Declare the use of a service.
	 * All callers of this method must call
	 * (@link #releaseService}
	 * or the service will not be released
	 *
	 * @deprecated use {@link #getServiceManager()} instead
	 * @param provider of the service
	 * @param <T> class of the service
	 * @return the service instance
	 */
	@Deprecated
	<T> T requestService(Class<? extends ServiceProvider<T>> provider);

	/**
	 * Release a service from duty. Each call to (@link #requestService} should be coupled with
	 * a call to (@link #releaseService} when the service is no longer needed.
	 *
	 * @deprecated use {@link #getServiceManager()} instead
	 * @param provider of the service
	 */
	@Deprecated
	void releaseService(Class<? extends ServiceProvider<?>> provider);

	/**
	 * Access the ServiceManager. Service users should keep a reference to the
	 * ServiceManager to allow for cleanup, but should never keep a reference to the
	 * BuildContext.
	 */
	ServiceManager getServiceManager();

	/**
	 * @return a reference to the IndexManagerHolder, storing all IndexManager instances.
	 */
	IndexManagerHolder getAllIndexesManager();

	/**
	 * For backends processing work asynchronously, they should catch all eventual errors in the ErrorHandler
	 * to avoid losing information about the lost updates.
	 *
	 * @return the configured ErrorHandler
	 */
	ErrorHandler getErrorHandler();
}
