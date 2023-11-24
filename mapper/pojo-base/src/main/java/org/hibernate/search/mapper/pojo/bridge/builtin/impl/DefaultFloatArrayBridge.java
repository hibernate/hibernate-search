/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.util.Arrays;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultFloatArrayBridge extends AbstractPassThroughDefaultBridge<float[]> {

	public static final DefaultFloatArrayBridge INSTANCE = new DefaultFloatArrayBridge();

	private DefaultFloatArrayBridge() {
	}

	@Override
	protected String toString(float[] value) {
		return Arrays.toString( value );
	}

	@Override
	protected float[] fromString(String value) {
		return ParseUtils.parseFloatPrimitiveArray( value );
	}

}
