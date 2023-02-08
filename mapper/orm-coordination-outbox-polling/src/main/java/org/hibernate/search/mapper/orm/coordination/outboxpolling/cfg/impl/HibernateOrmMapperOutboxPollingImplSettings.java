/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;

/**
 * Implementation-related settings, used for testing only.
 */
public final class HibernateOrmMapperOutboxPollingImplSettings {

	private HibernateOrmMapperOutboxPollingImplSettings() {
	}

	public static final String PREFIX = HibernateOrmMapperOutboxPollingSettings.PREFIX;

	public static final String COORDINATION_INTERNAL_CONFIGURER = PREFIX
			+ HibernateOrmMapperOutboxPollingSettings.Radicals.COORDINATION_PREFIX
			+ CoordinationRadicals.INTERNAL_CONFIGURER;

	public static final class CoordinationRadicals {

		private CoordinationRadicals() {
		}

		public static final String INTERNAL_CONFIGURER = "internal.configurer";
	}

}
