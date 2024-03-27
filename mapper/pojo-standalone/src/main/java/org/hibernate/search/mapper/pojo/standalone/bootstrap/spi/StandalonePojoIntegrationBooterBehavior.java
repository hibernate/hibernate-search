/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.bootstrap.spi;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class StandalonePojoIntegrationBooterBehavior {

	private StandalonePojoIntegrationBooterBehavior() {
	}

	// Exposed for override in native images, to make it extra-clear to SubstrateVM
	// that the native executable will never perform the first phase of the boot.
	public static <T> T bootFirstPhase(BootPhase<T> phase) {
		return phase.execute();
	}

	public interface BootPhase<T> {
		T execute();
	}
}
