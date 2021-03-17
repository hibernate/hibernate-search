/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.spi;

import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

public final class HibernateOrmMapperSpiSettings {

	private HibernateOrmMapperSpiSettings() {
	}

	public static final String PREFIX = HibernateOrmMapperSettings.PREFIX;

	public static final String INTEGRATION_PARTIAL_BUILD_STATE =
			PREFIX + Radicals.INTEGRATION_PARTIAL_BUILD_STATE;

	public static final String AUTOMATIC_INDEXING_PREFIX = PREFIX + "automatic_indexing.";

	/**
	 * Whether outbox events should be processed ({@code true})
	 * or left to accumulate without processing them ({@code false}).
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#AUTOMATIC_INDEXING_STRATEGY} is
	 * {@link AutomaticIndexingStrategyNames#OUTBOX_POLLING}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE}.
	 */
	public static final String AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE = AUTOMATIC_INDEXING_PREFIX
			+ AutomaticIndexingRadicals.PROCESS_OUTBOX_TABLE;

	public static class Radicals {

		private Radicals() {
		}

		public static final String INTEGRATION_PARTIAL_BUILD_STATE = "integration_partial_build_state";
	}

	/**
	 * Configuration property keys without the {@link #AUTOMATIC_INDEXING_PREFIX prefix}.
	 */
	public static final class AutomaticIndexingRadicals {

		private AutomaticIndexingRadicals() {
		}

		public static final String PROCESS_OUTBOX_TABLE = "process_outbox_table";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE = true;

	}

}
