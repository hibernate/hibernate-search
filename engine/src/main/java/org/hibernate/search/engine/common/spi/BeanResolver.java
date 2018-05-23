/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;


import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public interface BeanResolver extends AutoCloseable {

	/**
	 * Release any internal resource created while resolving beans.
	 * <p>
	 * Provided beans will not be usable after a call to this method.
	 *
	 * @see AutoCloseable#close()
	 */
	@Override
	void close();

	<T> T resolve(Class<?> implementationType, Class<T> expectedClass);

	<T> T resolve(String implementationName, Class<T> expectedClass);

	default <T> T resolve(BeanReference reference, Class<T> expectedClass) {
		String implementationName = reference.getName();
		Class<?> implementationType = reference.getType();
		boolean nameProvided = implementationName != null && !implementationName.isEmpty();
		boolean typeProvided = implementationType != null;

		if ( nameProvided && typeProvided ) {
			throw new SearchException( "This bean resolver does not support"
					+ " bean references using both a name and a type."
					+ " Got both '" + implementationName + "' and '" + implementationType + "' in the same reference" );
		}
		else if ( nameProvided ) {
			return resolve( implementationName, expectedClass );
		}
		else if ( typeProvided ) {
			return resolve( implementationType, expectedClass );
		}
		else {
			throw new SearchException( "Got an empty bean reference (no name, no type)" );
		}
	}

}
