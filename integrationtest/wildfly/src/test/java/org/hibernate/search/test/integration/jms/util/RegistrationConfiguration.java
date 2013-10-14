/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.integration.jms.util;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Davide D'Alto
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public final class RegistrationConfiguration {

	private static final Logger log = LoggerFactory.getLogger( RegistrationConfiguration.class );
	public static final String DESTINATION_QUEUE = "jms/queue/hsearch";
	private static final int MAX_ATTEMPTS = 3;

	private RegistrationConfiguration() {
		//not allowed
	}

	public static File createTempDir() {
		int attempts = 0;
		File baseDir = new File( System.getProperty( "java.io.tmpdir" ) );
		do {
			attempts++;
			String baseName = System.currentTimeMillis() + "_" + attempts;
			File tempDir = new File( baseDir, baseName );
			if ( tempDir.mkdir() ) {
				log.info( "Created tmp directory: " + tempDir.getAbsolutePath() );
				tempDir.deleteOnExit(); // delete the JVM exit, this way we don't have to bother about it
				return tempDir;
			}
		} while ( attempts < MAX_ATTEMPTS );

		throw new RuntimeException( "Impossible to create folder directory for indexes" );
	}
}
