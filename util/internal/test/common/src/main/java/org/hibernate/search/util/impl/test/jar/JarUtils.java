/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.jar;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.hibernate.search.util.impl.test.file.FileUtils;

public final class JarUtils {
	private JarUtils() {
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

	public static Path directoryToJar(Path sourceDir, Path targetDir) throws IOException {
		Path jarPath = targetDir.resolve( sourceDir.getFileName() + ".jar" ).toAbsolutePath();
		URI jarUri = URI.create( "jar:file:" + jarPath );

		Map<String, String> zipFsEnv = Collections.singletonMap( "create", "true" );
		try ( FileSystem zipFs = FileSystems.newFileSystem( jarUri, zipFsEnv ) ) {
			FileUtils.copyRecursively( sourceDir, zipFs.getRootDirectories().iterator().next() );
		}
		return jarPath;
	}
}
