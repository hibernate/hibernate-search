/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.util.Arrays;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public final class DefaultByteArrayBridge extends AbstractSimpleDefaultBridge<Byte[], byte[]> {

	public static final DefaultByteArrayBridge INSTANCE = new DefaultByteArrayBridge();

	private DefaultByteArrayBridge() {
	}

	@Override
	protected String toString(Byte[] value) {
		return Arrays.toString( value );
	}

	@Override
	protected Byte[] fromString(String value) {
		return ParseUtils.parseByteArray( value );
	}

	@Override
	public byte[] toIndexedValue(Byte[] value, ValueBridgeToIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		byte[] result = new byte[value.length];
		for ( int i = 0; i < value.length; i++ ) {
			result[i] = value[i];
		}
		return result;
	}

	@Override
	public Byte[] fromIndexedValue(byte[] value, ValueBridgeFromIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		Byte[] result = new Byte[value.length];
		for ( int i = 0; i < value.length; i++ ) {
			result[i] = Byte.valueOf( value[i] );
		}
		return result;
	}

	@Override
	public byte[] parse(String value) {
		return ParseUtils.parseBytePrimitiveArray( value );
	}
}
