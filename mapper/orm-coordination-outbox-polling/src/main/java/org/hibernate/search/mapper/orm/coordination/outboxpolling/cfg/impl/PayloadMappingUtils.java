/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl;

import java.util.Locale;

import org.hibernate.Length;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.PayloadType;
import org.hibernate.search.util.common.AssertionFailure;

public final class PayloadMappingUtils {
	private PayloadMappingUtils() {
	}

	private static final String MATERIALIZED_BLOB_TEMPLATE =
			"<property name=\"payload\" nullable=\"%s\" type=\"materialized_blob\"></property>";

	// We are using the `Length.LONG32` constant to make sure that ORM will try to use the `LONG32VARBINARY` if possible
	// and in particular with PostgreSQL it will result in bytea instead of lob types (oid)
	private static final String LONG32VARBINARY_TEMPLATE =
			"<property name=\"payload\" nullable=\"%s\" length=\"" + Length.LONG32 + "\"></property>";

	@SuppressWarnings("deprecation")
	public static String payload(PayloadType type, boolean nullable) {
		String template;
		switch ( type ) {
			case MATERIALIZED_BLOB:
				template = MATERIALIZED_BLOB_TEMPLATE;
				break;
			case LONG32VARBINARY:
				template = LONG32VARBINARY_TEMPLATE;
				break;
			default:
				throw new AssertionFailure( "Unsupported PayloadType: " + type );
		}
		return String.format( Locale.ROOT, template, nullable );
	}
}
