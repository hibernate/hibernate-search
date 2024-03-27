/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.index;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum IndexStatus {

	GREEN( "green" ),
	YELLOW( "yellow" ),
	RED( "red" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static IndexStatus of(String value) {
		return ParseUtils.parseDiscreteValues(
				IndexStatus.values(),
				IndexStatus::externalRepresentation,
				log::invalidIndexStatus,
				value
		);
	}

	private final String externalRepresentation;

	IndexStatus(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties,
	 * which happens to be the string representation of this status in Elasticsearch's REST API.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}
}
