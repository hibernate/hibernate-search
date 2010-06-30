package org.hibernate.search;

/**
 * This contract is considered experimental
 * Allow modifications of the SearchFactory internals
 *
 * As of today allow addition of new indexed classes.
 *
 * @author Emmanuel Bernard
 */
public interface IncrementalSearchFactory extends SearchFactory {
	/**
	 * Add the following classes to the SearchFactory
	 *
	 */
	void addClasses(Class<?>... classes);

	//TODO consider accepting SearchConfiguration or SearchMapping
}
