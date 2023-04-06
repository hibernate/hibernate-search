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

	public static final String INTEGRATION_PARTIAL_BUILD_STATE =
			PREFIX + Radicals.INTEGRATION_PARTIAL_BUILD_STATE;

	public static final String JBOSS_LOG_VERSION =
			// No Hibernate-specific prefix here; this controls the behavior of multiple JBoss libraries.
			"jboss.log-version";

	public static class Radicals {

		private Radicals() {
		}

		public static final String INTEGRATION_PARTIAL_BUILD_STATE = "integration_partial_build_state";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean JBOSS_LOG_VERSIONS = true;

	}

}
