/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.cfg;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public enum PayloadType {
	/**
	 * Using {@link org.hibernate.type.SqlTypes#MATERIALIZED_BLOB} to store the payload.
	 */
	@Deprecated
	MATERIALIZED_BLOB( "materialized_blob" ),

	/**
	 * Using {@link org.hibernate.type.SqlTypes#LONG32VARBINARY} to store the payload.
	 */
	LONG32VARBINARY( "long32varbinary" );

	private final String externalRepresentation;

	PayloadType(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String externalRepresentation() {
		return externalRepresentation;
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static PayloadType of(String value) {
		return ParseUtils.parseDiscreteValues(
				PayloadType.values(),
				PayloadType::externalRepresentation,
				log::invalidPayloadTypeName,
				value
		);
	}
}
