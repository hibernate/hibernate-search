/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.SearchFactory;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;

/**
 * This contract is considered experimental.
 *
 * This contract gives access to lower level APIs of Hibernate Search for
 * frameworks integrating with it. The piece of code creating the SearchFactory should
 * use this contract. It should however pass the higher level {@link org.hibernate.search.engine.SearchFactory} contract to
 * its clients.
 *
 * It also allows modification of some of the search factory internals:
 * - today allow addition of new indexed classes.
 *
 * @author Emmanuel Bernard
 * @experimental
 */
public interface SearchFactoryIntegrator extends SearchFactory {

	/**
	 * Returns the entity to index binding for the given type.
	 *
	 * @param entityType the type for which to retrieve the binding
	 *
	 * @return the entity to index binding for the given type. {@code null} is returned for types which are unindexed or
	 *         unknown.
	 */
	EntityIndexBinding getIndexBinding(Class<?> entityType);

	/**
	 * Add the following classes to the SearchFactory. If these classes are new to the SearchFactory this
	 * will trigger a reconfiguration.
	 */
	void addClasses(Class<?>... classes);

	//TODO consider accepting SearchConfiguration or SearchMapping

	Worker getWorker();

	void close();

	/**
	 * Return an Hibernate Search query object.
	 * This object uses fluent APIs to define the query executed.
	 * Offers a few execution approaches:
	 * - return the list of results eagerly
	 * - return the list of results lazily
	 * - get the number of results
	 */
	HSQuery createHSQuery();

	/**
	 * @return true if the SearchFactory was stopped
	 */
	boolean isStopped();

	/**
	 * Used to catch exceptions in all synchronous operations; but default they are logged, the user
	 * can configure alternative error management means.
	 *
	 * @return the configured ErrorHandler, global to the SearchFactory
	 */
	ErrorHandler getErrorHandler();

	TimeoutExceptionFactory getDefaultTimeoutExceptionFactory();

}
