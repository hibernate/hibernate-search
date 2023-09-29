/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

final class ParameterizedClassLifecycleStateExtension implements InvocationInterceptor {

	@Override

	public <T> T interceptTestClassConstructor(Invocation<T> invocation,
			ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext)
			throws Throwable {
		write(
				extensionContext,
				StoreKey.LIFECYCLE,
				detectParameterizedSetupLifecycle( invocationContext.getTargetClass() )
		);

		return InvocationInterceptor.super.interceptTestClassConstructor( invocation, invocationContext,
				extensionContext
		);
	}

	private ParameterizedSetup.Lifecycle detectParameterizedSetupLifecycle(Class<?> targetClass) {
		if ( Object.class.equals( targetClass ) ) {
			return null;
		}
		for ( Method method : targetClass.getDeclaredMethods() ) {
			Optional<ParameterizedSetup> annotation = findAnnotation( method, ParameterizedSetup.class );
			if ( annotation.isPresent() ) {
				return annotation.get().value();
			}
		}
		return detectParameterizedSetupLifecycle( targetClass.getSuperclass() );
	}

	public static Optional<ParameterizedSetup.Lifecycle> parameterizedSetupLifecycle(ExtensionContext context) {
		return Optional.ofNullable(
				read( context, StoreKey.LIFECYCLE, ParameterizedSetup.Lifecycle.class )
		);
	}

	private static void write(ExtensionContext context, StoreKey key, Object value) {
		ExtensionContext.Store store = context.getRoot().getStore( extensionNamespace( context ) );
		store.put( key, value );
	}

	private static <T> T read(ExtensionContext context, StoreKey key, Class<T> clazz) {
		ExtensionContext.Store store = context.getRoot().getStore( extensionNamespace( context ) );
		return store.get( key, clazz );
	}

	private static ExtensionContext.Namespace extensionNamespace(ExtensionContext context) {
		return ExtensionContext.Namespace.create(
				context.getRequiredTestClass(),
				ParameterizedClassLifecycleStateExtension.class
		);
	}

	private enum StoreKey {
		LIFECYCLE
	}
}
