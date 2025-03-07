/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.jar.impl;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.CommonMiscLog;

class CodeSource implements Closeable {

	private static final String JAR_URI_PATH_SEPARATOR = "!/";
	// Starting with Spring Boot 3.2 the nested jars in a repackaged app will be using an url structure as:
	//   * "jar:nested:/.../app-repackaged.jar/!BOOT-INF/classes/!/"
	//   * jar:nested:/.../app-repackaged.jar/!BOOT-INF/lib/some-packaged-lib.jar!/
	// that means that we will get the `nested:...` as a path when extracting the nested path.
	// Spring should be able to handle loading the files for such jars using their own custom filesystem and handlers
	// hence as soon as we discover such path we let it be handled by them.
	private static final String NESTED_SPEC_PREFIX = "nested:";
	private static final BiFunction<Path, URI, FileSystem> NESTED_JAR_FILESYSTEM_CREATOR;

	static {
		BiFunction<Path, URI, FileSystem> creator;
		try {
			Method newFileSystem = FileSystems.class.getMethod( "newFileSystem", Path.class, Map.class );
			creator = (path, jarUri) -> {
				try {
					return (FileSystem) newFileSystem.invoke( null, path, Collections.emptyMap() );
				}
				catch (IllegalAccessException | InvocationTargetException e) {
					throw CommonMiscLog.INSTANCE.cannotOpenNestedJar( jarUri, e );
				}
			};
		}
		catch (NoSuchMethodException ignored) {
			creator = (path, jarUri) -> {
				throw CommonMiscLog.INSTANCE.cannotOpenNestedJar( jarUri, null );
			};
		}
		NESTED_JAR_FILESYSTEM_CREATOR = creator;
	}

	private final List<FileSystem> fileSystems = new ArrayList<>();
	private final URL codeSourceLocation;
	private FileSystem nonDefaultFileSystem;
	private Path classesPathInFileSystem;

	CodeSource(URL codeSourceLocation) {
		this.codeSourceLocation = codeSourceLocation;
	}

	@SuppressForbiddenApis(reason = "URL constructors are deprecated in JDK 20+ in favor of using URI.toURL(),"
			+ " but we need to start from URLs (not URIs) coming from java.security.CodeSource.")
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
				Path rootResourcePath =
						nonDefaultFileSystem.getRootDirectories().iterator().next().resolve( resourcePathString );
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
			@SuppressWarnings("deprecation") // For JDK 20+
			// TODO: HSEARCH-4765 To be replaced with URL#of(URI, URLStreamHandler) when switching to JDK 20+
			// see https://download.java.net/java/early_access/jdk20/docs/api/java.base/java/net/URL.html#of(java.net.URI,java.net.URLStreamHandler) for deprecation info
			// cannot simply change to URI as boot specific Handler is required to make things work.
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
					URI jarUri = new URI( "jar:file", null, path.toUri().getPath(), null );
					tryInitJarFileSystem( jarUri );
				}
			}
			else {
				throw CommonMiscLog.INSTANCE.cannotInterpretCodeSourceUrl( codeSourceLocation );
			}
		}
		catch (RuntimeException | URISyntaxException | IOException e) {
			throw CommonMiscLog.INSTANCE.cannotOpenCodeSourceFileSystem( codeSourceLocation, e.getMessage(), e );
		}
	}

	private void tryInitJarFileSystem(URI jarUri) throws IOException {
		try {
			changeFileSystemAndMarkPreviousOneForClosing( FileSystems.newFileSystem( jarUri, Collections.emptyMap() ) );
			classesPathInFileSystem = nonDefaultFileSystem.getRootDirectories().iterator().next();
			// The ZipFileSystemProvider ignores the "path inside the JAR",
			// so we need to take care of that ourselves.
			Path nestedPath = extractedJarNestedPath( jarUri );
			if ( nestedPath != null && ( !Files.isRegularFile( nestedPath ) ) ) {
				classesPathInFileSystem = nestedPath;
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

	private Path extractedJarNestedPath(URI jarUri) {
		String spec = jarUri.getSchemeSpecificPart();
		if ( spec == null || spec.startsWith( NESTED_SPEC_PREFIX ) ) {
			return null;
		}
		int pathSeparatorIndex = spec.indexOf( JAR_URI_PATH_SEPARATOR );
		if ( pathSeparatorIndex < 0 ) {
			return null;
		}
		else {
			int afterPathSeparatorIndex = pathSeparatorIndex + JAR_URI_PATH_SEPARATOR.length();
			int secondPathSeparatorIndex = spec.indexOf( JAR_URI_PATH_SEPARATOR, afterPathSeparatorIndex );
			while ( 0 <= secondPathSeparatorIndex ) {
				Path nestedPathInFileSystem = classesPathInFileSystem.resolve(
						spec.substring( afterPathSeparatorIndex, secondPathSeparatorIndex )
				);
				if ( Files.isRegularFile( nestedPathInFileSystem ) ) {
					changeFileSystemAndMarkPreviousOneForClosing(
							NESTED_JAR_FILESYSTEM_CREATOR.apply( nestedPathInFileSystem, jarUri ) );
					classesPathInFileSystem = nonDefaultFileSystem.getRootDirectories().iterator().next();
				}
				else {
					return nestedPathInFileSystem;
				}
				afterPathSeparatorIndex = secondPathSeparatorIndex;
				secondPathSeparatorIndex = spec.indexOf( JAR_URI_PATH_SEPARATOR, afterPathSeparatorIndex );
			}
			return classesPathInFileSystem.resolve( spec.substring( afterPathSeparatorIndex ) );
		}
	}

	private void changeFileSystemAndMarkPreviousOneForClosing(FileSystem fileSystem) {
		if ( this.nonDefaultFileSystem != null ) {
			// we will be closing the filesystems in a reverse order:
			fileSystems.add( 0, nonDefaultFileSystem );
		}
		this.nonDefaultFileSystem = fileSystem;
	}

	@Override
	public void close() throws IOException {
		if ( nonDefaultFileSystem != null ) {
			changeFileSystemAndMarkPreviousOneForClosing( null );
		}
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.pushAll( FileSystem::close, fileSystems );
		}
	}
}
