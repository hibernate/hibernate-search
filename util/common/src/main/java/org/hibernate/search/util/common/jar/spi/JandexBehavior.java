/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.jar.spi;

public final class JandexBehavior {

	private JandexBehavior() {
	}

	// Exposed for override in native images, to make it extra-clear to SubstrateVM
	// that the native executable will never use Jandex.
	public static void doWithJandex(JandexOperation operation) {
		operation.execute();
	}

	public interface JandexOperation {
		void execute();
	}
}
