/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class ReflectionBeanResolver implements BeanResolver {

	@Override
	public <T> T resolve(Class<?> classOrFactoryClass, Class<T> expectedClass) {
		Object instance;
		try {
			instance = classOrFactoryClass.newInstance();
		}
		catch (InstantiationException|IllegalAccessException e) {
			throw new SearchException( "Error while resolving bean", e );
		}

		// TODO support @Factory annotation

		return expectedClass.cast( instance );
	}

	@Override
	public <T> T resolve(String implementationName, Class<T> expectedClass) {
		try {
			Class<?> classOrFactoryClass = getClass().getClassLoader().loadClass( implementationName );
			return resolve( classOrFactoryClass, expectedClass );
		}
		catch (ClassNotFoundException e) {
			throw new SearchException( "Error while resolving bean", e );
		}
	}

	@Override
	public <T> T resolve(BeanReference<? extends T> reference, Class<T> expectedClass) {
		String implementationName = reference.getName();
		Class<?> implementationType = reference.getType();
		boolean nameProvided = implementationName != null && !implementationName.isEmpty();
		boolean typeProvided = implementationType != null;

		if ( nameProvided && typeProvided ) {
			throw new SearchException( "The default, reflection-based bean resolver does not support"
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
