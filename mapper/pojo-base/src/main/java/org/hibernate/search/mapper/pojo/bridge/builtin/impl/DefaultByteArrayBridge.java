/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.util.Arrays;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.VectorBridge;

public final class DefaultByteArrayBridge extends AbstractPassThroughDefaultBridge<byte[]>
		implements VectorBridge<byte[], byte[]> {

	public static final DefaultByteArrayBridge INSTANCE = new DefaultByteArrayBridge();

	private DefaultByteArrayBridge() {
	}

	@Override
	protected String toString(byte[] value) {
		return Arrays.toString( value );
	}

	@Override
	protected byte[] fromString(String value) {
		return ParseUtils.parseBytePrimitiveArray( value );
	}
}
