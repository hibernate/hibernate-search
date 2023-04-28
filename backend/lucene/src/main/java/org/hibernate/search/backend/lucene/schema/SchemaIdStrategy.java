/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.schema;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Strategy that defines how to read/write IDs to/from a document.
 */
public enum SchemaIdStrategy {
	/**
	 * Currently used strategy that is compatible with the current and indexes from previous Hibernate Search versions.
	 * @deprecated It will be removed in a future version of Hibernate Search that will support Lucene 9.
	 * Use {@link #LUCENE_9} instead. Note that switching to {@link #LUCENE_9 a new strategy} will
	 * require indexes to be recreated and repopulated.
	 */
	@Deprecated
	LUCENE_8( "lucene8" ),
	/**
	 * This strategy will create documents compatible with future versions of Hibernate Search.
	 * Switching to this strategy for existing applications will require indexes to be recreated and repopulated.
	 */
	LUCENE_9( "lucene9" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String externalRepresentation;

	SchemaIdStrategy(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static SchemaIdStrategy of(String value) {
		return ParseUtils.parseDiscreteValues(
				SchemaIdStrategy.values(),
				SchemaIdStrategy::externalRepresentation,
				log::invalidLuceneIdStrategy,
				value
		);
	}
}
