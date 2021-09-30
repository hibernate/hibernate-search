/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.ZoneOffset;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultZoneOffsetBridge extends AbstractConvertingDefaultBridge<ZoneOffset, Integer> {

	@Override
	protected String toString(ZoneOffset value) {
		return value.getId();
	}

	@Override
	protected ZoneOffset fromString(String value) {
		return ParseUtils.parseZoneOffset( value );
	}

	@Override
	protected Integer toConvertedValue(ZoneOffset value) {
		return value.getTotalSeconds();
	}

	@Override
	protected ZoneOffset fromConvertedValue(Integer value) {
		return ZoneOffset.ofTotalSeconds( value );
	}

}
