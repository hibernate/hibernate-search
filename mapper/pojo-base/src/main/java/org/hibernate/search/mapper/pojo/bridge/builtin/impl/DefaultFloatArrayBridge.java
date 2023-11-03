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

public final class DefaultFloatArrayBridge extends AbstractSimpleDefaultBridge<Float[], float[]> {

	public static final DefaultFloatArrayBridge INSTANCE = new DefaultFloatArrayBridge();

	private DefaultFloatArrayBridge() {
	}

	@Override
	protected String toString(Float[] value) {
		return Arrays.toString( value );
	}

	@Override
	protected Float[] fromString(String value) {
		return ParseUtils.parseFloatArray( value );
	}

	@Override
	public float[] toIndexedValue(Float[] value, ValueBridgeToIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		float[] result = new float[value.length];
		for ( int i = 0; i < value.length; i++ ) {
			result[i] = value[i];
		}
		return result;
	}

	@Override
	public Float[] fromIndexedValue(float[] value, ValueBridgeFromIndexedValueContext context) {
		if ( value == null ) {
			return null;
		}
		Float[] result = new Float[value.length];
		for ( int i = 0; i < value.length; i++ ) {
			result[i] = Float.valueOf( value[i] );
		}
		return result;
	}

	@Override
	public float[] parse(String value) {
		return ParseUtils.parseFloatPrimitiveArray( value );
	}

}
