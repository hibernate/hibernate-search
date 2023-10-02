/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Date;
import java.time.Instant;

public final class DefaultJavaSqlDateBridge extends AbstractConvertingDelegatingDefaultBridge<Date, Instant> {

	public static final DefaultJavaSqlDateBridge INSTANCE = new DefaultJavaSqlDateBridge();

	public DefaultJavaSqlDateBridge() {
		super( DefaultInstantBridge.INSTANCE );
	}

	@Override
	protected Instant toConvertedValue(Date value) {
		return Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	protected Date fromConvertedValue(Instant value) {
		return new Date( value.toEpochMilli() );
	}

}
