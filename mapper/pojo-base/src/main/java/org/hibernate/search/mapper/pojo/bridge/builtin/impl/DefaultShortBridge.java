/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultShortBridge extends AbstractPassThroughDefaultBridge<Short> {

	public static final DefaultShortBridge INSTANCE = new DefaultShortBridge();

	private DefaultShortBridge() {
	}

	@Override
	protected String toString(Short value) {
		return value.toString();
	}

	@Override
	protected Short fromString(String value) {
		return ParseUtils.parseShort( value );
	}

}
