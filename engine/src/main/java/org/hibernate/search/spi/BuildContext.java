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
 * Build context that can be used by some services at initialization.
 *
 * @author Emmanuel Bernard
 */
public interface BuildContext {
	/**
	 * Returns the {@code SessionFactoryImplementor} instance. Do not use until after the initialize and/or start method is
	 * fully executed.
	 *
	 * Implementations should not cache values provided by the {@code SessionFactoryImplementor}, but rather access them
	 * each time, because the configuration can be dynamically updated and new changes made available.
	 *
	 * For example, prefer:
	 * <pre>
	 * {@code void method() {
	 *   int size = sfi.getDirectoryProviders().size();
	 * }
	 * }
	 * </pre>
	 * over
	 * <pre>
	 * {@code void method() {
	 * int size = directoryProviders.size();
	 * }
	 * }
	 * </pre>
	 * where directoryProviders is a class variable.
	 */
	SearchFactoryImplementor getUninitializedSearchFactory();

	/**
	 * Returns the configured indexing strategy (<i>event</i> vs <i>manual</i>).
	 *
	 * @return hte configured indexing strategy
	 * @see org.hibernate.search.Environment#INDEXING_STRATEGY
	 */
	String getIndexingStrategy();

	/**
	 * Declare the use of a service.
	 * All callers of this method must call
	 * (@link #releaseService}
	 * or the service will not be released
	 *
	 * @param provider of the service
	 * @param <T> class of the service
	 *
	 * @return the service instance
	 *
	 * @deprecated use {@link #getServiceManager()} instead
	 */
	@Deprecated
	<T> T requestService(Class<? extends ServiceProvider<T>> provider);

	/**
	 * Release a service from duty. Each call to (@link #requestService} should be coupled with
	 * a call to (@link #releaseService} when the service is no longer needed.
	 *
	 * @param provider of the service
	 *
	 * @deprecated use {@link #getServiceManager()} instead
	 */
	@Deprecated
	void releaseService(Class<? extends ServiceProvider<?>> provider);

	/**
	 * Access the {@code ServiceManager}.
	 *
	 * Clients should keep a reference to the {@code ServiceManager} to allow for cleanup, but should not keep a reference
	 * to the {@code BuildContext}.
	 */
	ServiceManager getServiceManager();

	/**
	 * @return a reference to the {@code IndexManagerHolder}, storing all {@code IndexManager} instances.
	 */
	IndexManagerHolder getAllIndexesManager();

	/**
	 * Back-ends processing work asynchronously should catch all eventual errors in the {@code ErrorHandler}
	 * to avoid losing information about the failing index updates.
	 *
	 * @return the configured {@code ErrorHandler}
	 */
	ErrorHandler getErrorHandler();
}
