/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Timestamp;
import java.time.Instant;

public final class DefaultJavaSqlTimestampBridge extends AbstractConvertingDelegatingDefaultBridge<Timestamp, Instant> {

	public static final DefaultJavaSqlTimestampBridge INSTANCE = new DefaultJavaSqlTimestampBridge();

	public DefaultJavaSqlTimestampBridge() {
		super( DefaultInstantBridge.INSTANCE );
	}

	@Override
	protected Instant toConvertedValue(Timestamp value) {
		return Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	protected Timestamp fromConvertedValue(Instant value) {
		return new Timestamp( value.toEpochMilli() );
	}

}
