/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cfg;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public enum UuidGenerationStrategy {
	/**
	 * Defaults to {@link #RANDOM}
	 */
	AUTO( "auto" ) {
		@Override
		public String strategy() {
			return RANDOM.strategy();
		}
	},
	/**
	 * Uses {@link UUID#randomUUID()} to generate values
	 */
	RANDOM( "random" ) {
		@Override
		public String strategy() {
			return UuidGenerator.Style.RANDOM.name();
		}
	},
	/**
	 * Applies a time-based generation strategy consistent with IETF RFC 4122.  Uses
	 * IP address rather than mac address.
	 * <p>
	 * NOTE : Can be a bottleneck due to the need to synchronize in order to increment an
	 * internal count as part of the algorithm.
	 */
	TIME( "time" ) {
		@Override
		public String strategy() {
			return UuidGenerator.Style.TIME.name();
		}
	};

	private final String externalRepresentation;

	UuidGenerationStrategy(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String externalRepresentation() {
		return externalRepresentation;
	}

	public abstract String strategy();

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static UuidGenerationStrategy of(String value) {
		return ParseUtils.parseDiscreteValues(
				UuidGenerationStrategy.values(),
				UuidGenerationStrategy::externalRepresentation,
				log::invalidUuidGenerationStrategyName,
				value
		);
	}
}
