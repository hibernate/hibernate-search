/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl;

/**
 * Implementation-related settings, used for testing only.
 */
public final class HibernateOrmMapperOutboxPollingImplSettings {

	private HibernateOrmMapperOutboxPollingImplSettings() {
	}

	public static final class CoordinationRadicals {

		private CoordinationRadicals() {
		}

		public static final String EVENT_PROCESSOR_AGENT_REPOSITORY_PROVIDER = "event_processor.agent_repository.provider";
		public static final String EVENT_PROCESSOR_OUTBOX_EVENT_FINDER_PROVIDER = "event_processor.outbox_event_finder.provider";
	}

}
