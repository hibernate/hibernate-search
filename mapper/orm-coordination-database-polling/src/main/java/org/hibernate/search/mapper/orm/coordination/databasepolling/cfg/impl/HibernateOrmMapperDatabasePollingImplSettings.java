/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.cfg.impl;

/**
 * Implementation-related settings, used for testing only.
 */
public final class HibernateOrmMapperDatabasePollingImplSettings {

	private HibernateOrmMapperDatabasePollingImplSettings() {
	}

	public static final class CoordinationRadicals {

		private CoordinationRadicals() {
		}

		public static final String PROCESSORS_INDEXING_OUTBOX_EVENT_FINDER_PROVIDER = "processors.indexing.outbox_event_finder.provider";
	}

}
