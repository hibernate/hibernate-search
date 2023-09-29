/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExecutableInvoker;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.runner.Description;

public final class JunitJupiterContextHelper {

	private JunitJupiterContextHelper() {
	}

	public static ExtensionContext extensionContext(Description description) {
		return new ExtensionContext() {
			private final UUID uuid = UUID.randomUUID();

			@Override
			public Optional<ExtensionContext> getParent() {
				return Optional.empty();
			}

			@Override
			public ExtensionContext getRoot() {
				return this;
			}

			@Override
			public String getUniqueId() {
				return uuid.toString();
			}

			@Override
			public String getDisplayName() {
				return description.getDisplayName();
			}

			@Override
			public Set<String> getTags() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<AnnotatedElement> getElement() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<Class<?>> getTestClass() {
				return Optional.of( description.getTestClass() );
			}

			@Override
			public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<Object> getTestInstance() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<TestInstances> getTestInstances() {
				return Optional.empty();
			}

			@Override
			public Optional<Method> getTestMethod() {
				try {
					return Optional.of( description.getTestClass().getMethod( description.getMethodName() ) );
				}
				catch (NoSuchMethodException e) {
					throw new IllegalStateException( e );
				}
			}

			@Override
			public Optional<Throwable> getExecutionException() {
				return Optional.empty();
			}

			@Override
			public Optional<String> getConfigurationParameter(String key) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> Optional<T> getConfigurationParameter(String key, Function<String, T> transformer) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void publishReportEntry(Map<String, String> map) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Store getStore(Namespace namespace) {
				return null;
			}

			@Override
			public ExecutionMode getExecutionMode() {
				throw new UnsupportedOperationException();
			}

			@Override
			public ExecutableInvoker getExecutableInvoker() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
