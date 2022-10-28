/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.jar.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.jar.JarTestUtils.toJar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.function.ThrowingConsumer;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.jar.JarFile;

public class JarUtilsTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void jarOrDirectoryPath_directory() throws Exception {
		Path dirPath = createDir( root -> {
			addSimpleClass( root );
		} );

		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( dirPath.toUri().toURL() ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			// Check precondition: this is what we want to test.
			assertThat( classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation().getProtocol() )
					.isEqualTo( "file" );

			// Check that the directory path is correctly inferred
			assertThat( JarUtils.jarOrDirectoryPath( classInIsolatedClassLoader ) )
					.contains( dirPath );
		}
	}

	@Test
	public void jarOrDirectoryPath_jar_fileScheme() throws Exception {
		Path jarPath = createJar( root -> {
			addSimpleClass( root );
		} );

		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( jarPath.toUri().toURL() ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			// Check preconditions: this is the situation that we want to test.
			URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
			assertThat( location.getProtocol() ).isEqualTo( "file" );
			assertThat( location.getPath() ).contains( jarPath.toString() );

			// Check that the JAR path is correctly inferred
			assertThat( JarUtils.jarOrDirectoryPath( classInIsolatedClassLoader ) )
					.contains( jarPath );
		}
	}

	@Test
	public void jarOrDirectoryPath_jar_classesInRoot() throws Exception {
		Path jarPath = createJar( root -> {
			addSimpleClass( root );
		} );

		URI fileURI = jarPath.toUri();
		URI jarURI = new URI( "jar", fileURI.getScheme() + ":" + fileURI.getSchemeSpecificPart() + "!/", null );
		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( jarURI.toURL() ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			// Check preconditions: this is the situation that we want to test.
			URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
			// For some reason the "jar" scheme gets replaced with "file"
			assertThat( location.getProtocol() ).isEqualTo( "file" );
			assertThat( location.getPath() ).contains( jarPath.toString() );

			// Check that the JAR path is correctly inferred
			assertThat( JarUtils.jarOrDirectoryPath( classInIsolatedClassLoader ) )
					.contains( jarPath );
		}
	}

	// Spring Boot, through its maven plugin, offers a peculiar JAR structure backed by a custom URL handler.
	// This tests that we correctly detect the path to the JAR in that case anyway.
	// See https://docs.spring.io/spring-boot/docs/2.2.13.RELEASE/maven-plugin//repackage-mojo.html
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4724")
	@Ignore // See HSEARCH-4724
	public void jarOrDirectoryPath_jar_classesInSubDirectory_springBoot() throws Exception {
		Path classesDirRelative = Paths.get( "BOOT-INF/classes" );
		Path jarPath = createJar( root -> {
			addSimpleClass( root.resolve( classesDirRelative ) );
		} );

		try ( JarFile outerJar = new JarFile( jarPath.toFile() ) ) {
			URL innerJarURL = new URL( outerJar.getUrl(), classesDirRelative + "!/" );
			try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( innerJarURL ) ) {
				Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

				// Check preconditions: this is the situation that we want to test.
				URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
				assertThat( location.getProtocol() ).isEqualTo( "jar" );
				assertThat( location.getPath() ).contains( classesDirRelative.toString() );

				// Check that the JAR path is correctly inferred
				assertThat( JarUtils.jarOrDirectoryPath( classInIsolatedClassLoader ) )
						.contains( jarPath );
			}
		}
	}

	// Spring Boot, through its maven plugin, offers a peculiar JAR structure backed by a custom URL handler.
	// This tests that we correctly detect the path to the JAR in that case anyway.
	// See https://docs.spring.io/spring-boot/docs/2.2.13.RELEASE/maven-plugin//repackage-mojo.html
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4724")
	@Ignore // See HSEARCH-4724
	public void jarOrDirectoryPath_jar_classesInSubJarInSubDirectory_springBoot() throws Exception {
		Path innerJarInOuterJarRelative = Paths.get( "BOOT-INF/lib/inner.jar" );
		// For some reason inner JAR entries in the outer JAR must not be compressed, otherwise classloading will fail.
		Path outerJarPath = createJar(
				Collections.singletonMap( "compressionMethod", "STORED" ),
				root -> {
					Path innerJar = createJar( innerJarRoot -> {
						addSimpleClass( innerJarRoot );
					} );
					Path innerJarInOuterJarAbsolute = root.resolve( innerJarInOuterJarRelative );
					Files.createDirectories( innerJarInOuterJarAbsolute.getParent() );
					Files.copy( innerJar, innerJarInOuterJarAbsolute );
				}
		);

		try ( JarFile outerJar = new JarFile( outerJarPath.toFile() ) ) {
			URL innerJarURL = outerJar.getNestedJarFile( outerJar.getJarEntry( innerJarInOuterJarRelative.toString() ) )
					.getUrl();
			try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( innerJarURL ) ) {
				Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );
				// Check preconditions: this is the situation that we want to test.
				URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
				assertThat( location.getProtocol() ).isEqualTo( "jar" );
				assertThat( location.getPath() ).contains( innerJarInOuterJarRelative.toString() );

				// Check that the JAR path is correctly inferred
				assertThat( JarUtils.jarOrDirectoryPath( classInIsolatedClassLoader ) )
						.contains( outerJarPath );
			}
		}
	}

	@Test
	public void openJarOrDirectory_directory() throws Exception {
		Path dirPath = createDir( root -> {
			Files.createFile( root.resolve( "someFile.txt" ) );
		} );
		try ( FileSystem jarFs = JarUtils.openJarOrDirectory( dirPath ) ) {
			assertThat( jarFs ).isNull();
		}
	}

	@Test
	public void openJarOrDirectory_jar() throws Exception {
		Path jarPath = createJar( root -> {
			Files.createFile( root.resolve( "someFile.txt" ) );
		} );

		try ( FileSystem jarFs = JarUtils.openJarOrDirectory( jarPath ) ) {
			assertThat( jarFs ).isNotNull();
			Path jarRoot = jarFs.getRootDirectories().iterator().next();
			assertThat( Files.exists( jarRoot.resolve( "someFile.txt" ) ) ).isTrue();
		}
	}

	@Test
	public void openJarOrDirectory_jar_specialCharacter() throws Exception {
		Path initialJarPath = createJar( root -> {
			Files.createFile( root.resolve( "someFile.txt" ) );
		} );
		Path jarPath = Files.copy(
				initialJarPath,
				temporaryFolder.newFolder().toPath().resolve( "namewith@specialchar.jar" )
		);

		try ( FileSystem jarFs = JarUtils.openJarOrDirectory( jarPath ) ) {
			assertThat( jarFs ).isNotNull();
			Path jarRoot = jarFs.getRootDirectories().iterator().next();
			assertThat( Files.exists( jarRoot.resolve( "someFile.txt" ) ) ).isTrue();
		}
	}

	private Path createDir(ThrowingConsumer<Path, IOException> contributor) throws IOException {
		Path dirPath = temporaryFolder.newFolder().toPath();
		contributor.accept( dirPath );
		return dirPath;
	}

	private Path createJar(ThrowingConsumer<Path, IOException> contributor) throws IOException {
		return createJar( null, contributor );
	}

	private Path createJar(Map<String, String> zipFsEnv, ThrowingConsumer<Path, IOException> contributor)
			throws IOException {
		return toJar( temporaryFolder, createDir( contributor ), zipFsEnv );
	}

	private String resourceName(Class<?> clazz) {
		return clazz.getName().replaceAll( "\\.", "/" ) + ".class";
	}

	private void addSimpleClass(Path classesDir) throws IOException {
		String classResourceName = resourceName( SimpleClass.class );
		Path classFile = classesDir.resolve( Paths.get( classResourceName ) );
		Files.createDirectories( classFile.getParent() );
		try ( InputStream stream = getClass().getClassLoader().getResourceAsStream( classResourceName ) ) {
			Files.copy( stream, classFile );
		}
	}

	private static URLClassLoader createIsolatedClassLoader(URL jarURL) {
		return new URLClassLoader( new URL[] { jarURL }, null );
	}

}
