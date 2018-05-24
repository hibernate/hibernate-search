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
public final class ReflectionBeanResolver implements BeanResolver {

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> T resolve(Class<?> classOrFactoryClass, Class<T> expectedClass) {
		Object instance;
		try {
			instance = classOrFactoryClass.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new SearchException( "Error while resolving bean", e );
		}

		// TODO support @Factory annotation

		return expectedClass.cast( instance );
	}

	@Override
	public <T> T resolve(String classOrFactoryClassName, Class<T> expectedClass) {
		try {
			Class<?> classOrFactoryClass = getClass().getClassLoader().loadClass( classOrFactoryClassName );
			return resolve( classOrFactoryClass, expectedClass );
		}
		catch (ClassNotFoundException e) {
			throw new SearchException( "Error while resolving bean", e );
		}
	}

	@Override
	public <T> T resolve(String nameReference, Class<?> typeReference, Class<T> expectedClass) {
		throw new SearchException( "This bean resolver does not support"
				+ " bean references using both a name and a type."
				+ " Got both '" + nameReference + "' and '" + typeReference + "' in the same reference" );
	}

}
