/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultIntegerBridge extends AbstractPassThroughDefaultBridge<Integer> {

	public static final DefaultIntegerBridge INSTANCE = new DefaultIntegerBridge();

	private DefaultIntegerBridge() {
	}

	@Override
	protected String toString(Integer value) {
		return value.toString();
	}

	@Override
	protected Integer fromString(String value) {
		return ParseUtils.parseInteger( value );
	}

}
