/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.File;
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

	static void initializeWriteableDirectory(Path directory) throws IOException {
		File directoryFile = directory.toFile();
		if ( directoryFile.exists() ) {
			if ( !directoryFile.isDirectory() || !Files.isWritable( directory ) ) {
				throw log.pathIsNotWriteableDirectory( directory );
			}
		}
		else {
			log.indexDirectoryNotFoundCreatingNewOne( directory );
			Files.createDirectories( directory );
		}
	}

}
