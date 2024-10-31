/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cfg;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.ConfigurationLog;
import org.hibernate.search.util.common.annotation.Incubating;

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

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static PayloadType of(String value) {
		return ParseUtils.parseDiscreteValues(
				PayloadType.values(),
				PayloadType::externalRepresentation,
				ConfigurationLog.INSTANCE::invalidPayloadTypeName,
				value
		);
	}
}
