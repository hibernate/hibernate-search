/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.Instant;
import java.util.Date;

public final class DefaultJavaUtilDateBridge extends AbstractConvertingDelegatingDefaultBridge<Date, Instant> {

	public static final DefaultJavaUtilDateBridge INSTANCE = new DefaultJavaUtilDateBridge();

	public DefaultJavaUtilDateBridge() {
		super( DefaultInstantBridge.INSTANCE );
	}

	@Override
	protected Instant toConvertedValue(Date value) {
		// java.sql.* types do not support toInstant(). See HSEARCH-3670
		return Instant.ofEpochMilli( value.getTime() );
	}

	@Override
	protected Date fromConvertedValue(Instant value) {
		return Date.from( value );
	}

}
