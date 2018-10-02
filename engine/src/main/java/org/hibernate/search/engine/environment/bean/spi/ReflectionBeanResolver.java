/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;

import org.hibernate.search.engine.environment.classpath.spi.ClassLoaderHelper;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public final class ReflectionBeanResolver implements BeanResolver {

	private final ClassResolver classResolver;

	public ReflectionBeanResolver(ClassResolver classResolver) {
		this.classResolver = classResolver;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> T resolve(Class<?> classOrFactoryClass, Class<T> expectedClass) {
		Object instance = ClassLoaderHelper.untypedInstanceFromClass( classOrFactoryClass, expectedClass.getName() );

		// TODO support @Factory annotation

		return expectedClass.cast( instance );
	}

	@Override
	public <T> T resolve(String classOrFactoryClassName, Class<T> expectedClass) {
		Class<?> classOrFactoryClass = ClassLoaderHelper.classForName(
				expectedClass, classOrFactoryClassName, expectedClass.getName(), classResolver
		);

		return resolve( classOrFactoryClass, expectedClass );
	}

	@Override
	public <T> T resolve(String nameReference, Class<?> typeReference, Class<T> expectedClass) {
		throw new SearchException( "This bean resolver does not support"
				+ " bean references using both a name and a type."
				+ " Got both '" + nameReference + "' and '" + typeReference + "' in the same reference" );
	}

}
