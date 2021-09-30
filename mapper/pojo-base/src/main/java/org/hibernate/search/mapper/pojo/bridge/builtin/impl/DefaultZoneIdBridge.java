/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZoneId;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultZoneIdBridge extends AbstractStringBasedDefaultBridge<ZoneId> {

	@Override
	protected String toString(ZoneId value) {
		return value.getId();
	}

	@Override
	protected ZoneId fromString(String value) {
		return ParseUtils.parseZoneId( value );
	}

}
