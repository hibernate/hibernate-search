/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZoneOffset;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultZoneOffsetBridge extends AbstractConvertingDefaultBridge<ZoneOffset, Integer> {

	public static final DefaultZoneOffsetBridge INSTANCE = new DefaultZoneOffsetBridge();

	private DefaultZoneOffsetBridge() {
	}

	@Override
	protected String toString(ZoneOffset value) {
		return value.getId();
	}

	@Override
	protected ZoneOffset fromString(String value) {
		return ParseUtils.parseZoneOffset( value );
	}

	@Override
	protected Integer toConvertedValue(ZoneOffset value) {
		return value.getTotalSeconds();
	}

	@Override
	protected ZoneOffset fromConvertedValue(Integer value) {
		return ZoneOffset.ofTotalSeconds( value );
	}

}
