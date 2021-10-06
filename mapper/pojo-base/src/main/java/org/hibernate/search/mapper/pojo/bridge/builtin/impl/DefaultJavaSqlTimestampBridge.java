/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Timestamp;
import java.time.Instant;

public final class DefaultJavaSqlTimestampBridge extends AbstractConvertingDelegatingDefaultBridge<Timestamp, Instant> {

	public static final DefaultJavaSqlTimestampBridge INSTANCE = new DefaultJavaSqlTimestampBridge();

	public DefaultJavaSqlTimestampBridge() {
		super( DefaultInstantBridge.INSTANCE );
	}

	@Override
	protected Instant toConvertedValue(Timestamp value) {
		return Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	protected Timestamp fromConvertedValue(Instant value) {
		return new Timestamp( value.toEpochMilli() );
	}

}
