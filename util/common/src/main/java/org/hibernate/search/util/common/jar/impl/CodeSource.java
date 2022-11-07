/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.jar.impl;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class CodeSource implements Closeable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String JAR_URI_PATH_SEPARATOR = "!/";
	private final URL codeSourceLocation;
	private FileSystem nonDefaultFileSystem;
	private Path classesPathInFileSystem;

	CodeSource(URL codeSourceLocation) {
		this.codeSourceLocation = codeSourceLocation;
	}

	public InputStream readOrNull(String resourcePathString) throws IOException {
		Throwable exception = null;

		// First, try to convert the URL to a filesystem and access the resource there.
		// This will only work in some well-known scenarios, but the most likely ones,
		// such as when the code source is a directory on the default filesystem,
		// or a non-nested JAR.
		try {
			Path relativeResourcePath = classesPathOrFail().resolve( resourcePathString );
			if ( Files.exists( relativeResourcePath ) ) {
				return Files.newInputStream( relativeResourcePath );
			}
			// For JARs, also try to find the resource at the root:
			// in some cases, such as Spring Boot's repackaged JARs,
			// class files live in a subdirectory, e.g. `BOOT-INF/classes`,
			// but meta-inf still lives at the root.
			if ( nonDefaultFileSystem != null ) {
				Path rootResourcePath = nonDefaultFileSystem.getRootDirectories().iterator().next().resolve( resourcePathString );
				if ( Files.exists( rootResourcePath ) ) {
					return Files.newInputStream( rootResourcePath );
				}
			}
			// We didn't find the file.
			return null;
		}
		catch (RuntimeException | IOException e) {
			exception = Throwables.combine( exception, e );
		}

		// As a last resort try to access the URL directly:
		// this won't work in most cases, but might save us in some exotic cases
		// such as a nested JAR.
		try {
			URL resourceUrl = new URL( codeSourceLocation, resourcePathString );
			return resourceUrl.openStream();
		}
		catch (FileNotFoundException e) {
			return null;
		}
		catch (RuntimeException | IOException e) {
			exception = Throwables.combine( exception, e );
		}

		throw new IOException(
				"Could not open '" + resourcePathString + "' within '" + codeSourceLocation + "': " + exception.getMessage(),
				exception
		);
	}

	public Path classesPathOrFail() throws IOException {
		initFileSystem();
		return classesPathInFileSystem;
	}

	void initFileSystem() throws IOException {
		if ( classesPathInFileSystem != null ) {
			return;
		}

		try {
			if ( "jar".equals( codeSourceLocation.getProtocol() ) ) {
				tryInitJarFileSystem( codeSourceLocation.toURI() );
			}
			else if ( "file".equals( codeSourceLocation.getProtocol() ) ) {
				Path path = Paths.get( codeSourceLocation.toURI() );
				if ( Files.isDirectory( path ) ) {
					// The URI points to a directory, e.g. target/classes in Maven builds.
					// This may happen when running tests with maven-surefire-plugin, in particular.
					// We'll access the directory as-is.
					nonDefaultFileSystem = null;
					classesPathInFileSystem = path;
				}
				else {
					// The URI points to a regular file, so hopefully an actual JAR file.
					// We'll try to open a ZIP filesystem to work on the contents of the JAR file.
					URI jarUri = new URI( "jar:file", null, path.toString(), null );
					tryInitJarFileSystem( jarUri );
				}
			}
			else {
				throw log.cannotInterpretCodeSourceUrl( codeSourceLocation );
			}
		}
		catch (RuntimeException | URISyntaxException | IOException e) {
			throw log.cannotOpenCodeSourceFileSystem( codeSourceLocation, e.getMessage(), e );
		}
	}

	private void tryInitJarFileSystem(URI jarUri) throws IOException {
		try {
			nonDefaultFileSystem = FileSystems.newFileSystem( jarUri, Collections.emptyMap() );
			classesPathInFileSystem = nonDefaultFileSystem.getRootDirectories().iterator().next();
			// The ZipFileSystemProvider ignores the "path inside the JAR",
			// so we need to take care of that ourselves.
			String nestedPath = extractedJarNestedPath( jarUri );
			if ( nestedPath != null ) {
				Path nestedPathInFileSystem = classesPathInFileSystem.resolve( nestedPath );
				if ( Files.isRegularFile( nestedPathInFileSystem ) ) {
					// TODO HSEARCH-4744 support reading the content of nested JARs
					throw log.cannotOpenNestedJar( jarUri );
				}
				classesPathInFileSystem = nestedPathInFileSystem;
			}
		}
		catch (RuntimeException | IOException e) {
			new SuppressingCloser( e )
					.push( nonDefaultFileSystem );
			nonDefaultFileSystem = null;
			classesPathInFileSystem = null;
			throw e;
		}
	}

	private String extractedJarNestedPath(URI jarUri) throws IOException {
		String spec = jarUri.getSchemeSpecificPart();
		if ( spec == null ) {
			return null;
		}
		int pathSeparatorIndex = spec.indexOf( JAR_URI_PATH_SEPARATOR );
		if ( pathSeparatorIndex < 0 ) {
			return null;
		}
		else {
			int afterPathSeparatorIndex = pathSeparatorIndex + JAR_URI_PATH_SEPARATOR.length();
			int secondPathSeparatorIndex = spec.indexOf( JAR_URI_PATH_SEPARATOR, afterPathSeparatorIndex );
			if ( 0 <= secondPathSeparatorIndex && secondPathSeparatorIndex + JAR_URI_PATH_SEPARATOR.length() < spec.length() ) {
				// TODO HSEARCH-4744 support reading the content of nested JARs
				throw log.cannotOpenNestedJar( jarUri );
			}
			return spec.substring( afterPathSeparatorIndex, secondPathSeparatorIndex );
		}
	}

	@Override
	public void close() throws IOException {
		if ( nonDefaultFileSystem != null ) {
			nonDefaultFileSystem.close();
		}
	}
}
