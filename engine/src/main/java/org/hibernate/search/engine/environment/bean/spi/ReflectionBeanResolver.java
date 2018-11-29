/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.classpath.spi.ClassLoaderHelper;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public final class ReflectionBeanResolver implements BeanResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ClassResolver classResolver;

	public ReflectionBeanResolver(ClassResolver classResolver) {
		this.classResolver = classResolver;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> T resolve(Class<T> expectedClass, Class<?> classOrFactoryClass) {
		Object instance = ClassLoaderHelper.untypedInstanceFromClass( classOrFactoryClass, expectedClass.getName() );

		// TODO support @Factory annotation

		return expectedClass.cast( instance );
	}

	@Override
	public <T> T resolve(Class<T> expectedClass, String classOrFactoryClassName) {
		Class<?> classOrFactoryClass = ClassLoaderHelper.classForName(
				expectedClass, classOrFactoryClassName, expectedClass.getName(), classResolver
		);

		return resolve( expectedClass, classOrFactoryClass );
	}

	@Override
	public <T> T resolve(Class<T> expectedClass, String nameReference, Class<?> typeReference) {
		throw log.resolveBeanUsingBothNameAndType( nameReference, typeReference );
	}

}
