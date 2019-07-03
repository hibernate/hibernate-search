/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZoneId;

import org.hibernate.search.engine.cfg.spi.ValidateUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultZoneIdValueBridge implements ValueBridge<ZoneId, String> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public String toIndexedValue(ZoneId value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.getId();
	}

	@Override
	public ZoneId fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : ZoneId.of( value );
	}

	@Override
	public ZoneId cast(Object value) {
		return (ZoneId) value;
	}

	@Override
	public String parse(String value) {
		ValidateUtils.validateZoneId( value );
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

}
