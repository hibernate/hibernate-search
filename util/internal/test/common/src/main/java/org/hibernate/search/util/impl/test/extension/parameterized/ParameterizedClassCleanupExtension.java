/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import static org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedClassExtension.areThereMoreTestsForCurrentConfigurations;

import org.hibernate.search.util.impl.test.extension.ExtensionScope;
import org.hibernate.search.util.impl.test.function.ThrowingConsumer;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * An extension that triggers cleanup after all tests for a configuration are executed.
 * <p>
 * Since, because of the wrapping extension behaviour (see {@link AfterEachCallback Wrapping Behavior}),
 * and as {@link ParameterizedSetup} is registered after the {@code @RegisterExtension SomeExtension extension = ... }
 * the after each callback of a parameterized setup is executed before the ones from the other extensions.
 * <p>
 * Adding an {@link AfterEachCallback} to the {@link ParameterizedClass} means that we are registering the callback
 * sooner than any {@code @RegisterExtension SomeExtension extension = ... } leading to it being executed after the callbacks from
 * other registered extensions.
 */
final class ParameterizedClassCleanupExtension implements AfterEachCallback {
	public void afterEach(ExtensionContext context) throws Exception {
		if ( areThereMoreTestsForCurrentConfigurations( context ) ) {
			// There are no more tests to execute for the current configuration, so we want to run a cleanup
			// for any resources configured in the parameterized setup:
			ThrowingConsumer<ExtensionScope, Exception> cleanUp = ExtensionScope.scopeCleanUp( context );
			cleanUp.accept( ExtensionScope.PARAMETERIZED_CLASS_SETUP );
		}
	}
}
