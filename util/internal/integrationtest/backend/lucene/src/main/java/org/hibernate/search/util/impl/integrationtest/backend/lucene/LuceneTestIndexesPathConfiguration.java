/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene;

import java.nio.file.Paths;

import org.jboss.logging.Logger;

public class LuceneTestIndexesPathConfiguration {

	private static final Logger log = Logger.getLogger( LuceneTestIndexesPathConfiguration.class.getName() );

	private static LuceneTestIndexesPathConfiguration instance;

	public static LuceneTestIndexesPathConfiguration get() {
		if ( instance == null ) {
			instance = new LuceneTestIndexesPathConfiguration();
		}
		return instance;
	}

	private final String path;

	private LuceneTestIndexesPathConfiguration() {
		String pathFromSystemProperties = System.getProperty( "test.lucene.indexes.path" );
		if ( pathFromSystemProperties == null ) {
			// Happens when running tests from the IDE.
			// Assume tests are run from the module's root directory.
			this.path = Paths.get( "target/test-indexes" ).toAbsolutePath().toString();
		}
		else {
			this.path = pathFromSystemProperties;
		}

		log.infof(
				"Integration tests will put indexes in directory '%s'",
				path
		);
	}

	public String getPath() {
		return path;
	}
}
