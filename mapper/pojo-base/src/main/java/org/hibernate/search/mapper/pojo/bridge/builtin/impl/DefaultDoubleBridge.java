/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultDoubleBridge extends AbstractPassThroughDefaultBridge<Double> {

	public static final DefaultDoubleBridge INSTANCE = new DefaultDoubleBridge();

	private DefaultDoubleBridge() {
	}

	@Override
	protected String toString(Double value) {
		return value.toString();
	}

	@Override
	protected Double fromString(String value) {
		return ParseUtils.parseDouble( value );
	}

}
