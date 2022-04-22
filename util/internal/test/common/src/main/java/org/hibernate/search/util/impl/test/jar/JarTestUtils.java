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
import java.util.Map;

import org.hibernate.search.util.impl.test.file.FileUtils;

import org.junit.rules.TemporaryFolder;

public final class JarTestUtils {
	private JarTestUtils() {
	}

	public static Path determineJarOrDirectoryLocation(Class<?> classFromJar, String jarName) {
		URL url = classFromJar.getProtectionDomain().getCodeSource().getLocation();
		if ( !url.getProtocol().equals( "file" ) ) {
			throw new IllegalStateException( jarName + " JAR is not a local file? " + url );
		}
		try {
			return Paths.get( url.toURI() );
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException( e );
		}
	}

	public static Path toJar(TemporaryFolder temporaryFolder, Path jarOrDirectoryPath) {
		if ( Files.isRegularFile( jarOrDirectoryPath ) ) {
			return jarOrDirectoryPath;
		}
		try {
			Path tempDir = temporaryFolder.newFolder().toPath();
			Path jarPath = tempDir.resolve( jarOrDirectoryPath.getFileName() + ".jar" ).toAbsolutePath();
			URI jarUri = new URI( "jar:file", null, jarPath.toUri().getPath(), null );
			Map<String, String> zipFsEnv = Collections.singletonMap( "create", "true" );
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
