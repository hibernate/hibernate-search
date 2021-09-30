/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.sql.Date;
import java.time.Instant;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;

public final class DefaultJavaSqlDateBridge extends AbstractConvertingDelegatingDefaultBridge<Date, Instant> {

	public static final DefaultJavaSqlDateBridge INSTANCE = new DefaultJavaSqlDateBridge();

	public DefaultJavaSqlDateBridge() {
		super( DefaultInstantBridge.INSTANCE );
	}

	@Override
	protected Instant toConvertedValue(Date value) {
		return Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	protected Date fromConvertedValue(Instant value) {
		return new Date( value.toEpochMilli() );
	}

}
