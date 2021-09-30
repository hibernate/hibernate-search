/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Time;
import java.time.Instant;

public final class DefaultJavaSqlTimeBridge extends AbstractConvertingDelegatingDefaultBridge<Time, Instant> {

	public DefaultJavaSqlTimeBridge() {
		super( new DefaultInstantBridge() );
	}

	@Override
	protected Instant toConvertedValue(Time value) {
		return Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	protected Time fromConvertedValue(Instant value) {
		return new Time( value.toEpochMilli() );
	}

}
