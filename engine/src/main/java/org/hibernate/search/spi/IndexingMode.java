/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Modes for triggering indexing of entities.
 *
 * @author Gunnar Morling
 */
public enum IndexingMode {

	/**
	 * Indexing is triggered automatically upon entity insertion, update etc.
	 */
	EVENT("event"),

	/**
	 * Indexing is triggered explicitly.
	 */
	MANUAL("manual");

	private static Log LOG = LoggerFactory.make();

	private String externalRepresentation;

	private IndexingMode(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * Returns the {@link IndexingMode} matching the given external representation as specified via
	 * {@link org.hibernate.search.cfg.Environment#INDEXING_STRATEGY}
	 * @param indexingMode the indexing mode external representation
	 * @return the {@link IndexingMode}
	 */
	public static IndexingMode fromExternalRepresentation(String indexingMode) {
		if ( EVENT.toExternalRepresentation().equals( indexingMode ) ) {
			return IndexingMode.EVENT;
		}
		else if ( MANUAL.toExternalRepresentation().equals( indexingMode ) ) {
			return IndexingMode.MANUAL;
		}
		else {
			throw LOG.unknownIndexingMode( indexingMode );
		}
	}

	/**
	 * Returns the external representation of this indexing mode. Generally this enumeration itself should preferably be
	 * used for comparisons etc.
	 * @return the external representation as string
	 */
	public String toExternalRepresentation() {
		return externalRepresentation;
	}
}
