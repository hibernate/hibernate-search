/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class FileSystemUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private FileSystemUtils() {
	}

	static void initializeWriteableDirectory(Path rootDirectory) throws IOException {
		if ( Files.exists( rootDirectory ) ) {
			if ( !Files.isDirectory( rootDirectory ) || !Files.isWritable( rootDirectory ) ) {
				throw log.pathIsNotWriteableDirectory( rootDirectory );
			}
		}
		else {
			Files.createDirectories( rootDirectory );
		}
	}

}
