/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.mapper.pojo.standalone.reporting.impl.StandalonePojoEventContextMessages;

public final class StandalonePojoMappingKey
		implements MappingKey<StandalonePojoMappingPartialBuildState, StandalonePojoMapping> {
	private static final StandalonePojoEventContextMessages MESSAGES = StandalonePojoEventContextMessages.INSTANCE;

	@Override
	public String render() {
		return MESSAGES.mapping();
	}
}
