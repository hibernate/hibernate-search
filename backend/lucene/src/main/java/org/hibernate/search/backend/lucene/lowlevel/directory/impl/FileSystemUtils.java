/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.logging.impl.LuceneSpecificLog;

final class FileSystemUtils {

	private FileSystemUtils() {
	}

	static void initializeWriteableDirectory(Path directory) throws IOException {
		File directoryFile = directory.toFile();
		if ( directoryFile.exists() ) {
			if ( !directoryFile.isDirectory() || !Files.isWritable( directory ) ) {
				throw LuceneSpecificLog.INSTANCE.pathIsNotWriteableDirectory( directory );
			}
		}
		else {
			LuceneSpecificLog.INSTANCE.indexDirectoryNotFoundCreatingNewOne( directory );
			Files.createDirectories( directory );
		}
	}

}
