/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;


import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.classpath.spi.ClassLoaderHelper;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;



public final class ReflectionBeanProvider implements BeanProvider {

	private final ClassResolver classResolver;

	public ReflectionBeanProvider(ClassResolver classResolver) {
		this.classResolver = classResolver;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference) {
		return BeanHolder.of( resolveNoClosingNecessary( typeReference ) );
	}

	public <T> T resolveNoClosingNecessary(Class<T> typeReference) {
		return ClassLoaderHelper.untypedInstanceFromClass( typeReference, typeReference.getName() );
	}

	@Override
	public <T> BeanHolder<T> resolve(Class<T> typeReference, String implementationFullyQualifiedClassName) {
		return BeanHolder.of( resolveNoClosingNecessary( typeReference, implementationFullyQualifiedClassName ) );
	}

	public <T> T resolveNoClosingNecessary(Class<T> typeReference, String implementationFullyQualifiedClassName) {
		Class<? extends T> implementationClass = ClassLoaderHelper.classForName(
				typeReference, implementationFullyQualifiedClassName, typeReference.getName(), classResolver
		);
		return ClassLoaderHelper.untypedInstanceFromClass( implementationClass, implementationFullyQualifiedClassName );
	}

}
