/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

import org.hibernate.id.uuid.CustomVersionOneStrategy;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
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
			return StandardRandomStrategy.class.getName();
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
			return CustomVersionOneStrategy.class.getName();
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
