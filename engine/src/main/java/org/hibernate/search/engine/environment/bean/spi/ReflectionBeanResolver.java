/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;


import org.hibernate.search.engine.environment.classpath.spi.ClassLoaderHelper;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;


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
	public <T> T resolve(Class<T> typeReference) {
		return ClassLoaderHelper.untypedInstanceFromClass( typeReference, typeReference.getName() );
	}

	@Override
	public <T> T resolve(Class<T> typeReference, String implementationFullyQualifiedClassName) {
		Class<? extends T> implementationClass = ClassLoaderHelper.classForName(
				typeReference, implementationFullyQualifiedClassName, typeReference.getName(), classResolver
		);

		return resolve( implementationClass );
	}

}
