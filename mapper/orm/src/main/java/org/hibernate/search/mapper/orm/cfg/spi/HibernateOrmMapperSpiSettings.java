/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.spi;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

public final class HibernateOrmMapperSpiSettings {

	private HibernateOrmMapperSpiSettings() {
	}

	public static final String PREFIX = HibernateOrmMapperSettings.PREFIX;

	/**
	 * The strategy to follow when performing reflection operations,
	 * in particular when calling methods or accessing field dynamically.
	 * <p>
	 * Expects a {@link HibernateOrmReflectionStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#REFLECTION_STRATEGY}.
	 */
	public static final String REFLECTION_STRATEGY = PREFIX + Radicals.REFLECTION_STRATEGY;

	public static final String INTEGRATION_PARTIAL_BUILD_STATE =
			PREFIX + Radicals.INTEGRATION_PARTIAL_BUILD_STATE;

	public static class Radicals {

		private Radicals() {
		}

		public static final String REFLECTION_STRATEGY = "reflection_strategy";

		public static final String INTEGRATION_PARTIAL_BUILD_STATE = "integration_partial_build_state";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final HibernateOrmReflectionStrategyName REFLECTION_STRATEGY =
				HibernateOrmReflectionStrategyName.METHOD_HANDLE;
	}

}
