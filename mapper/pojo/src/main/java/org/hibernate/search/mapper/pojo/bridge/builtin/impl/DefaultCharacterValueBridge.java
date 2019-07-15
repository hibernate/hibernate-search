/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ValidateUtils;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultCharacterValueBridge implements ValueBridge<Character, String> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public String toIndexedValue(Character value, ValueBridgeToIndexedValueContext context) {
		// The character is turned into a one character String
		return value == null ? null : Character.toString( value );
	}

	@Override
	public Character fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		return ( value == null || value.isEmpty() ) ? null : value.charAt( 0 );
	}

	@Override
	public String parse(String value) {
		ValidateUtils.validateCharacter( value );
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}
}
