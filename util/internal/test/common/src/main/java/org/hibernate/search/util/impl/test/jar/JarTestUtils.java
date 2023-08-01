/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.jar;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.util.impl.test.file.FileUtils;

import org.junit.rules.TemporaryFolder;

public final class JarTestUtils {
	private JarTestUtils() {
	}

	private static Path toPath(URL codeSourceLocation) {
		try {
			return Paths.get( codeSourceLocation.toURI() );
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( "Cannot convert URL '" + codeSourceLocation + "' to Path", e );
		}
	}

	public static Path toJar(TemporaryFolder temporaryFolder, URL codeSourceLocation) {
		return toJar( temporaryFolder, toPath( codeSourceLocation ) );
	}

	public static Path toJar(TemporaryFolder temporaryFolder, Path jarOrDirectoryPath) {
		return toJar( temporaryFolder, jarOrDirectoryPath, null );
	}

	public static Path toJar(TemporaryFolder temporaryFolder, Path jarOrDirectoryPath, Map<String, String> additionalZipFsEnv) {
		if ( Files.isRegularFile( jarOrDirectoryPath ) ) {
			return jarOrDirectoryPath;
		}
		try {
			Path tempDir = temporaryFolder.newFolder().toPath();
			Path jarPath = tempDir.resolve( jarOrDirectoryPath.getFileName() + ".jar" ).toAbsolutePath();
			URI jarUri = new URI( "jar:file", null, jarPath.toUri().getPath(), null );
			Map<String, String> zipFsEnv = new HashMap<>();
			zipFsEnv.put( "create", "true" );
			if ( Runtime.version().feature() < 17 ) {
				// tests that run on JDK11 and try to use Spring Boot 2.7 cannot work with a compressed jar,
				// so we try to create it with no compression:
				zipFsEnv.put( "noCompression", "true" );
			}
			if ( additionalZipFsEnv != null ) {
				zipFsEnv.putAll( additionalZipFsEnv );
			}
			try ( FileSystem jarFs = FileSystems.newFileSystem( jarUri, zipFsEnv ) ) {
				FileUtils.copyRecursively( jarOrDirectoryPath, jarFs.getRootDirectories().iterator().next() );
			}
			return jarPath;
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Exception turning " + jarOrDirectoryPath + " into a JAR: " + e.getMessage(), e );
		}
	}

	public static Path toDirectory(TemporaryFolder temporaryFolder, URL codeSourceLocation) {
		return toDirectory( temporaryFolder, toPath( codeSourceLocation ) );
	}

	public static Path toDirectory(TemporaryFolder temporaryFolder, Path jarOrDirectoryPath) {
		if ( Files.isDirectory( jarOrDirectoryPath ) ) {
			return jarOrDirectoryPath;
		}
		try {
			Path tempDir = temporaryFolder.newFolder().toPath();
			URI jarUri = new URI( "jar:file", null, jarOrDirectoryPath.toUri().getPath(), null );
			Map<String, String> zipFsEnv = Collections.emptyMap();
			try ( FileSystem jarFs = FileSystems.newFileSystem( jarUri, zipFsEnv ) ) {
				FileUtils.copyRecursively( jarFs.getRootDirectories().iterator().next(), tempDir );
			}
			return tempDir;
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Exception turning " + jarOrDirectoryPath + " into an unpacked directory: " + e.getMessage(), e );
		}
	}

}
