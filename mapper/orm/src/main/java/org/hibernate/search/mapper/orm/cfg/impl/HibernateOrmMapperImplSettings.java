/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg.impl;

/**
 * Implementation-related settings, used for testing only.
 */
public final class HibernateOrmMapperImplSettings {

	private HibernateOrmMapperImplSettings() {
	}

	public static final class AutomaticIndexingRadicals {

		private AutomaticIndexingRadicals() {
		}

		public static final String OUTBOX_EVENT_FINDER = "outbox_event_finder";
	}

}
