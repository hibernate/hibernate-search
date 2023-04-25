/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public enum FileSystemAccessStrategyName {

	AUTO( "auto" ),
	NIO( "nio" ),
	MMAP( "mmap" );

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// This method conforms to the MicroProfile Config specification. Do not change its signature.
	public static FileSystemAccessStrategyName of(String value) {
		return ParseUtils.parseDiscreteValues(
				FileSystemAccessStrategyName.values(),
				FileSystemAccessStrategyName::externalRepresentation,
				log::invalidFileSystemAccessStrategyName,
				value
		);
	}

	private final String externalRepresentation;

	FileSystemAccessStrategyName(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	/**
	 * @return The expected string representation in configuration properties.
	 */
	public String externalRepresentation() {
		return externalRepresentation;
	}
}
