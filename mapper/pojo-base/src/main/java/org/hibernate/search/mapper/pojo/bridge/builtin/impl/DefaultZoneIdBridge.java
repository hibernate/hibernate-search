/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZoneId;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultZoneIdBridge extends AbstractStringBasedDefaultBridge<ZoneId> {

	public static final DefaultZoneIdBridge INSTANCE = new DefaultZoneIdBridge();

	private DefaultZoneIdBridge() {
	}

	@Override
	protected String toString(ZoneId value) {
		return value.getId();
	}

	@Override
	protected ZoneId fromString(String value) {
		return ParseUtils.parseZoneId( value );
	}

}
