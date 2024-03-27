/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cfg.impl;

import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;

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
