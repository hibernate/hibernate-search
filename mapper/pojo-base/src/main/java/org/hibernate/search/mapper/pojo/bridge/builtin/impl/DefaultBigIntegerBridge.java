/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.math.BigInteger;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultBigIntegerBridge extends AbstractPassThroughDefaultBridge<BigInteger> {

	public static final DefaultBigIntegerBridge INSTANCE = new DefaultBigIntegerBridge();

	private DefaultBigIntegerBridge() {
	}

	@Override
	protected String toString(BigInteger value) {
		return value.toString();
	}

	@Override
	protected BigInteger fromString(String value) {
		return ParseUtils.parseBigInteger( value );
	}

}
