/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.util.UUID;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultUUIDBridge extends AbstractStringBasedDefaultBridge<UUID> {

	public static final DefaultUUIDBridge INSTANCE = new DefaultUUIDBridge();

	private DefaultUUIDBridge() {
	}

	@Override
	protected String toString(UUID value) {
		return value.toString();
	}

	@Override
	protected UUID fromString(String value) {
		return ParseUtils.parseUUID( value );
	}

}
