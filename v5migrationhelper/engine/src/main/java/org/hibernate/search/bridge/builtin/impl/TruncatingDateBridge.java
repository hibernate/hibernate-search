/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.bridge.builtin.impl;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class TruncatingDateBridge implements ValueBridge<Date, Instant> {

	private final Truncation truncation;

	public TruncatingDateBridge(Truncation truncation) {
		this.truncation = truncation;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Instant toIndexedValue(Date value, ValueBridgeToIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		return truncate( Instant.ofEpochMilli( value.getTime() ) );
	}

	@Override
	public Date fromIndexedValue(Instant value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : Date.from( value );
	}

	@Override
	public Instant parse(String value) {
		return truncate( ParseUtils.parseInstant( value ) );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() )
				&& truncation.equals( ( (TruncatingDateBridge) other ).truncation );
	}

	private Instant truncate(Instant value) {
		ZonedDateTime zonedDateTime = value.atZone( ZoneOffset.UTC );
		return truncation.truncate( zonedDateTime ).toInstant();
	}

}
