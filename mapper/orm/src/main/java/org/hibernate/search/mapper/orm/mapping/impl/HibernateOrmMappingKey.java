/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.mapper.orm.reporting.impl.HibernateOrmEventContextMessages;

public final class HibernateOrmMappingKey
		implements MappingKey<HibernateOrmMappingPartialBuildState, HibernateOrmMapping> {

	private static final HibernateOrmEventContextMessages MESSAGES = HibernateOrmEventContextMessages.INSTANCE;

	@Override
	public String render() {
		return MESSAGES.mapping();
	}

}
