/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.cfg.spi;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class StandalonePojoMapperSpiSettings {

	private StandalonePojoMapperSpiSettings() {
	}

	public static final String PREFIX = "hibernate.search.";

	public static final String BEAN_PROVIDER = PREFIX + Radicals.BEAN_PROVIDER;

	public static final String INTEGRATION_PARTIAL_BUILD_STATE =
			PREFIX + Radicals.INTEGRATION_PARTIAL_BUILD_STATE;

	public static class Radicals {

		private Radicals() {
		}

		public static final String BEAN_PROVIDER = "bean_provider";

		public static final String INTEGRATION_PARTIAL_BUILD_STATE = "integration_partial_build_state";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}
	}

}
