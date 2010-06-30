package org.hibernate.search.spi;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.Worker;

/**
 * This contract is considered experimental.
 *
 * This contract gives access to lower level APIs of Hibernate Search for
 * frameworks integrating with it. The piece of code creating the SearchFactory should
 * use this contract. It should however pass the higherlevel {@link }SearchFactory} contract to
 * its clients.
 *
 * It also allows modification of some of the search factory internals:
 *  - today allow addition of new indexed classes.
 *
 * @author Emmanuel Bernard
 */
public interface SearchFactoryIntegrator extends SearchFactory {
	/**
	 * Add the following classes to the SearchFactory
	 *
	 */
	void addClasses(Class<?>... classes);

	//TODO consider accepting SearchConfiguration or SearchMapping

	Worker getWorker();

	void close();
}
