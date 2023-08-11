/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.PayloadType;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.type.SqlTypes;

public final class PayloadMappingUtils {
	private PayloadMappingUtils() {
	}

	@SuppressWarnings("deprecation")
	public static int payload(PayloadType type) {
		switch ( type ) {
			case MATERIALIZED_BLOB:
				return SqlTypes.MATERIALIZED_BLOB;
			case LONG32VARBINARY:
				// TODO: check with Yoann
				//  note it's not the `LONG32VARBINARY` ... Postgres driver doesn't know about such type and simply fails
				//  to process events ...
				return SqlTypes.LONGVARBINARY;
			default:
				throw new AssertionFailure( "Unsupported PayloadType: " + type );
		}
	}
}
