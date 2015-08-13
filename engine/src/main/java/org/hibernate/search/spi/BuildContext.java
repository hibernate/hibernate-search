/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.impl.IndexManagerHolder;

/**
 * Build context that can be used by some services at initialization.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface BuildContext {
	/**
	 * Returns the {@code ExtendedSearchintegrator} instance. Do not use until after the initialize and/or start method is
	 * fully executed.
	 *
	 * Implementations should not cache values provided by the {@code ExtendedSearchintegrator}, but rather access them
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
	 * @return the {@link ExtendedSearchIntegrator} instance
	 */
	ExtendedSearchIntegrator getUninitializedSearchIntegrator();

	/**
	 * @deprecated Scheduled for removal. Use {@link #getIndexingMode()} instead.
	 * @return the indexing strategy
	 */
	@Deprecated
	String getIndexingStrategy();

	/**
	 * @return the current indexing strategy as specified via {@link org.hibernate.search.cfg.Environment#INDEXING_STRATEGY}.
	 */
	IndexingMode getIndexingMode();

	/**
	 * Access the {@code ServiceManager}.
	 *
	 * Clients should keep a reference to the {@code ServiceManager} to allow for cleanup, but should not keep a reference
	 * to the {@code BuildContext}.
	 *
	 * @return the {@link ServiceManager}
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
