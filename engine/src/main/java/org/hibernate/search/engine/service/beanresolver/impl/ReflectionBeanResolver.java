/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.beanresolver.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.search.annotations.Factory;
import org.hibernate.search.engine.service.beanresolver.spi.BeanResolver;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * A bean resolver that uses reflection to instantiate beans.
 * <p>
 * The class passed as an argument must expose a public, no-arg constructor,
 * or this resolver will fail with a {@link SearchException}.
 * <p>
 * If the class passed as an argument contains a method annotated
 * with {@code @Factory}, the class will be considered as a factory
 * and the returned bean will be fetched from this factory method instead.
 */
public class ReflectionBeanResolver implements BeanResolver {

	private static final Log log = LoggerFactory.make();

	@Override
	public <T> T resolve(Class<?> classOrFactoryClass, Class<T> expectedClass) {
		Object instance;
		try {
			instance = classOrFactoryClass.newInstance();
		}
		catch (InstantiationException e) {
			throw log.noPublicNoArgConstructor( classOrFactoryClass.getName() );
		}
		catch (IllegalAccessException e) {
			throw log.unableToAccessClass( classOrFactoryClass.getName() );
		}

		// check for a factory annotation
		int numberOfFactoryMethodsFound = 0;
		for ( Method method : classOrFactoryClass.getMethods() ) {
			if ( method.isAnnotationPresent( Factory.class ) ) {
				if ( numberOfFactoryMethodsFound == 1 ) {
					throw log.multipleFactoryMethodsInClass( classOrFactoryClass.getName() );
				}
				if ( method.getReturnType() == void.class ) {
					throw log.factoryMethodsMustReturnAnObject( classOrFactoryClass.getName(), method.getName() );
				}
				try {
					instance = method.invoke( instance );
				}
				catch (IllegalAccessException e) {
					throw log.unableToAccessMethod( classOrFactoryClass.getName(), method.getName() );
				}
				catch (InvocationTargetException e) {
					throw log.exceptionDuringFactoryMethodExecution( e, classOrFactoryClass.getName(), method.getName() );
				}
				numberOfFactoryMethodsFound++;
			}
		}
		return expectedClass.cast( instance );
	}

}
