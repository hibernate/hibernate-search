/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.jar.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Manifest;

import org.hibernate.search.util.common.AssertionFailure;

public final class JarUtils {
	private JarUtils() {
	}

	private static final String META_INF_MANIFEST = "META-INF/MANIFEST.MF";


	/*
	 * Code originally released under ASL 2.0.
	 * <p>
	 * Original code:
	 * https://github.com/quarkusio/quarkus/blob/8d4d3459b01203d2ce35d7847874a88941960443/independent-projects/bootstrap/core/src/main/java/io/quarkus/bootstrap/classloading/JarClassPathElement.java#L37-L56
	 */
	private static final int JAVA_VERSION;

	static {
		int version = 8;
		try {
			Method versionMethod = Runtime.class.getMethod( "version" );
			Object v = versionMethod.invoke( null );
			@SuppressWarnings({ "unchecked", "raw" })
			List<Integer> list = (List<Integer>) v.getClass().getMethod( "version" ).invoke( v );
			version = list.get( 0 );
		}
		catch (Exception e) {
			//version 8
		}
		JAVA_VERSION = version;
	}

	public static int javaVersion() {
		return JAVA_VERSION;
	}

	public static Optional<Path> jarOrDirectoryPath(Class<?> classFromJar) {
		CodeSource codeSource = classFromJar.getProtectionDomain().getCodeSource();
		if ( codeSource == null ) {
			return Optional.empty();
		}
		URL url = codeSource.getLocation();
		if ( url == null || !url.getProtocol().equals( "file" ) ) {
			return Optional.empty();
		}
		try {
			Path path = Paths.get( url.toURI() );
			return Optional.of( path );
		}
		catch (URISyntaxException e) {
			throw new AssertionFailure( "Unexpected failure while accessing JAR", e );
		}
	}

	public static FileSystem openJarOrDirectory(Path jarOrDirectoryPath) throws IOException, URISyntaxException {
		if ( Files.isDirectory( jarOrDirectoryPath ) ) {
			// The JAR is a directory, e.g. target/classes in Maven builds.
			// This may happens when running tests with maven-surefire-plugin, in particular.
			// We'll use the directory as-is.
			return null;
		}
		else {
			// This is a regular file, so hopefully an actual JAR file.
			// We'll open a ZIP filesystem to work on the contents of the JAR file.
			URI jarUri = new URI( "jar:file", null, jarOrDirectoryPath.toUri().getPath(), null );
			Map<String, String> zipFsEnv = Collections.emptyMap();
			return FileSystems.newFileSystem( jarUri, zipFsEnv );
		}
	}

	/**
	 * Code originally released under ASL 2.0.
	 * <p>
	 * Original code: https://github.com/smallrye/smallrye-common/blob/main/io/src/main/java/io/smallrye/common/io/jar/JarFiles.java#L45-L60
	 */
	static boolean isMultiRelease(Path jarRoot) {
		String value;

		Path manifestPath = jarRoot.resolve( META_INF_MANIFEST );
		if ( !Files.exists( manifestPath ) ) {
			return false;
		}

		try {
			Manifest manifest = new Manifest( Files.newInputStream( manifestPath ) );
			value = manifest.getMainAttributes().getValue( "Multi-Release" );
		}
		catch (IOException var3) {
			throw new UncheckedIOException( "Cannot read manifest attributes", var3 );
		}

		return Boolean.parseBoolean( value );
	}

}
