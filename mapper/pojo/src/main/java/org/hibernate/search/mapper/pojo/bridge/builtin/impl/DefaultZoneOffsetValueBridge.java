/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZoneOffset;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultZoneOffsetValueBridge implements ValueBridge<ZoneOffset, Integer> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Integer toIndexedValue(ZoneOffset value, ValueBridgeToIndexedValueContext context) {
		return toIndexedValue( value );
	}

	@Override
	public ZoneOffset fromIndexedValue(Integer value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : ZoneOffset.ofTotalSeconds( value );
	}

	@Override
	public Integer parse(String value) {
		return toIndexedValue( ParseUtils.parseZoneOffset( value ) );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private Integer toIndexedValue(ZoneOffset value) {
		return value == null ? null : value.getTotalSeconds();
	}

}
