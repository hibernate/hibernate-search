/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.jar.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.jar.JarTestUtils.toJar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.function.ThrowingConsumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.jar.JarFile;

public class CodeSourceTest {

	private static final String META_INF_FILE_RELATIVE_PATH = "META-INF/someFile.txt";
	private static final byte[] META_INF_FILE_CONTENT = "This is some content".getBytes( StandardCharsets.UTF_8 );
	private static final String NON_EXISTING_FILE_RELATIVE_PATH = "META-INF/nonExisting.txt";
	private static final String SIMPLE_CLASS_RELATIVE_PATH = SimpleClass.class.getName().replaceAll(
			"\\.", "/" ) + ".class";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void directory() throws Exception {
		Path dirPath = createDir( root -> {
			addMetaInfFile( root );
			addSimpleClass( root );
		} );

		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( dirPath.toUri().toURL() ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			// Check preconditions: this is the situation that we want to test.
			URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
			assertThat( location.getProtocol() ).isEqualTo( "file" );
			assertThat( location.toExternalForm() ).contains( dirPath.toString() );

			// Check that the JAR can be opened and that we can access other files within it
			try ( CodeSource codeSource = new CodeSource( location ) ) {
				try ( InputStream is = codeSource.readOrNull( META_INF_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).hasBinaryContent( META_INF_FILE_CONTENT );
				}
				try ( InputStream is = codeSource.readOrNull( NON_EXISTING_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).isNull();
				}
				Path classesPath = codeSource.classesPathOrFail();
				try ( Stream<Path> files = Files.walk( classesPath ).filter( Files::isRegularFile ) ) {
					assertThat( files )
							.containsExactlyInAnyOrder(
									classesPath.resolve( META_INF_FILE_RELATIVE_PATH ),
									classesPath.resolve( SIMPLE_CLASS_RELATIVE_PATH )
							);
				}
			}
		}
	}

	@Test
	public void jar_fileScheme() throws Exception {
		Path jarPath = createJar( root -> {
			addMetaInfFile( root );
			addSimpleClass( root );
		} );

		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( jarPath.toUri().toURL() ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			// Check preconditions: this is the situation that we want to test.
			URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
			assertThat( location.getProtocol() ).isEqualTo( "file" );
			assertThat( location.toExternalForm() ).contains( jarPath.toString() );

			// Check that the JAR can be opened and that we can access other files within it
			try ( CodeSource codeSource = new CodeSource( location ) ) {
				try ( InputStream is = codeSource.readOrNull( META_INF_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).hasBinaryContent( META_INF_FILE_CONTENT );
				}
				try ( InputStream is = codeSource.readOrNull( NON_EXISTING_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).isNull();
				}
				Path classesPath = codeSource.classesPathOrFail();
				try ( Stream<Path> files = Files.walk( classesPath ).filter( Files::isRegularFile ) ) {
					assertThat( files )
							.containsExactlyInAnyOrder(
									classesPath.resolve( META_INF_FILE_RELATIVE_PATH ),
									classesPath.resolve( SIMPLE_CLASS_RELATIVE_PATH )
							);
				}
			}
		}
	}

	@Test
	public void jar_jarScheme_classesInRoot() throws Exception {
		Path jarPath = createJar( root -> {
			addMetaInfFile( root );
			addSimpleClass( root );
		} );

		URI fileURL = jarPath.toUri();
		URL jarURL = new URI( "jar:" + fileURL + "!/" ).toURL();
		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( jarURL ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			// Check preconditions: this is the situation that we want to test.
			URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
			// For some reason the "jar" scheme gets replaced with "file"
			assertThat( location.getProtocol() ).isEqualTo( "file" );
			assertThat( location.toExternalForm() ).contains( jarPath.toString() );

			// Check that the JAR can be opened and that we can access other files within it
			try ( CodeSource codeSource = new CodeSource( location ) ) {
				try ( InputStream is = codeSource.readOrNull( META_INF_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).hasBinaryContent( META_INF_FILE_CONTENT );
				}
				try ( InputStream is = codeSource.readOrNull( NON_EXISTING_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).isNull();
				}
				Path classesPath = codeSource.classesPathOrFail();
				try ( Stream<Path> files = Files.walk( classesPath ).filter( Files::isRegularFile ) ) {
					assertThat( files )
							.containsExactlyInAnyOrder(
									classesPath.resolve( META_INF_FILE_RELATIVE_PATH ),
									classesPath.resolve( SIMPLE_CLASS_RELATIVE_PATH )
							);
				}
			}
		}
	}

	// Spring Boot, through its maven plugin, offers a peculiar JAR structure backed by a custom URL handler.
	// This tests that we correctly detect the path to the JAR in that case anyway.
	// See https://docs.spring.io/spring-boot/docs/2.2.13.RELEASE/maven-plugin//repackage-mojo.html
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4724")
	public void jar_jarScheme_springBoot_classesInSubDirectory() throws Exception {
		String classesDirRelativeString = "BOOT-INF/classes";
		Path jarPath = createJar( root -> {
			addMetaInfFile( root );
			addSimpleClass( root.resolve( classesDirRelativeString ) );
		} );

		try ( JarFile outerJar = new JarFile( jarPath.toFile() ) ) {
			@SuppressWarnings( "deprecation" ) // For JDK 20+
			// TODO: HSEARCH-4765 To be replaced with URL#of(URI, URLStreamHandler) when switching to JDK 20+
			// see https://download.java.net/java/early_access/jdk20/docs/api/java.base/java/net/URL.html#of(java.net.URI,java.net.URLStreamHandler) for deprecation info
			// cannot simply change to URI as boot specific Handler is required to make things work.
			URL innerJarURL = new URL( outerJar.getUrl(), classesDirRelativeString + "!/" );
			try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( innerJarURL ) ) {
				Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

				// Check preconditions: this is the situation that we want to test.
				URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
				assertThat( location.getProtocol() ).isEqualTo( "jar" );
				assertThat( location.toExternalForm() ).contains( classesDirRelativeString );

				// Check that the JAR can be opened and that we can access other files within it
				try ( CodeSource codeSource = new CodeSource( location ) ) {
					try ( InputStream is = codeSource.readOrNull( META_INF_FILE_RELATIVE_PATH ) ) {
						assertThat( is ).hasBinaryContent( META_INF_FILE_CONTENT );
					}
					try ( InputStream is = codeSource.readOrNull( NON_EXISTING_FILE_RELATIVE_PATH ) ) {
						assertThat( is ).isNull();
					}
					Path classesPath = codeSource.classesPathOrFail();
					Path jarRoot = classesPath.getRoot();
					try ( Stream<Path> files = Files.walk( jarRoot ).filter( Files::isRegularFile ) ) {
						assertThat( files )
								.containsExactlyInAnyOrder(
										jarRoot.resolve( META_INF_FILE_RELATIVE_PATH ),
										classesPath.resolve( SIMPLE_CLASS_RELATIVE_PATH )
								);
					}
				}
			}
		}
	}

	// Spring Boot, through its maven plugin, offers a peculiar JAR structure backed by a custom URL handler.
	// This tests that we correctly detect the path to the (outer) JAR in that case anyway,
	// and can retrieve content from that JAR.
	// See https://docs.spring.io/spring-boot/docs/2.2.13.RELEASE/maven-plugin//repackage-mojo.html
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4724")
	public void jar_jarScheme_springBoot_classesInSubJarInSubDirectory() throws Exception {
		String innerJarInOuterJarRelativePathString = "BOOT-INF/lib/inner.jar";
		// For some reason inner JAR entries in the outer JAR must not be compressed, otherwise classloading will fail.
		Path outerJarPath = createJar(
				Collections.singletonMap( "compressionMethod", "STORED" ),
				root -> {
					Path innerJar = createJar( innerJarRoot -> {
						addMetaInfFile( innerJarRoot );
						addSimpleClass( innerJarRoot );
					} );
					Path innerJarInOuterJarAbsolute = root.resolve( innerJarInOuterJarRelativePathString );
					Files.createDirectories( innerJarInOuterJarAbsolute.getParent() );
					Files.copy( innerJar, innerJarInOuterJarAbsolute );
				}
		);

		try ( JarFile outerJar = new JarFile( outerJarPath.toFile() ) ) {
			URL innerJarURL = outerJar.getNestedJarFile( outerJar.getJarEntry( innerJarInOuterJarRelativePathString ) )
					.getUrl();
			try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( innerJarURL ) ) {
				Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );
				// Check preconditions: this is the situation that we want to test.
				URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
				assertThat( location.getProtocol() ).isEqualTo( "jar" );
				assertThat( location.toExternalForm() ).contains( innerJarInOuterJarRelativePathString );

				// Check that the JAR can be opened and that we can access other files within it
				try ( CodeSource codeSource = new CodeSource( location ) ) {
					try ( InputStream is = codeSource.readOrNull( META_INF_FILE_RELATIVE_PATH ) ) {
						assertThat( is ).hasBinaryContent( META_INF_FILE_CONTENT );
					}
					try ( InputStream is = codeSource.readOrNull( NON_EXISTING_FILE_RELATIVE_PATH ) ) {
						assertThat( is ).isNull();
					}
					// TODO HSEARCH-4744 support reading the content of nested JARs
					assertThatThrownBy( codeSource::classesPathOrFail )
							.isInstanceOf( IOException.class )
							.hasMessageContainingAll(
									"Cannot open filesystem for code source at",
									location.toString(),
									"URI points to content inside a nested JAR"
							);
				}
			}
		}
	}

	@Test
	public void jar_jarScheme_specialCharacter() throws Exception {
		Path initialJarPath = createJar( root -> {
			addMetaInfFile( root );
			addSimpleClass( root );
		} );
		Path parentDirWithSpecialChar = temporaryFolder.newFolder().toPath()
				.resolve( "parentnamewith%40special@char" );
		Files.createDirectories( parentDirWithSpecialChar );
		Path jarPath = Files.copy(
				initialJarPath,
				parentDirWithSpecialChar.resolve( "namewith%40special@char.jar" )
		);

		URI fileURL = jarPath.toUri();
		URL jarURL = new URI( "jar:" + fileURL + "!/" ).toURL();
		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( jarURL ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
			// Check that the JAR can be opened and that we can access other files within it
			try ( CodeSource codeSource = new CodeSource( location ) ) {
				try ( InputStream is = codeSource.readOrNull( META_INF_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).hasBinaryContent( META_INF_FILE_CONTENT );
				}
				try ( InputStream is = codeSource.readOrNull( NON_EXISTING_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).isNull();
				}
				Path jarRoot = codeSource.classesPathOrFail();
				try ( Stream<Path> files = Files.walk( jarRoot ).filter( Files::isRegularFile ) ) {
					assertThat( files )
							.containsExactlyInAnyOrder(
									jarRoot.resolve( META_INF_FILE_RELATIVE_PATH ),
									jarRoot.resolve( SIMPLE_CLASS_RELATIVE_PATH )
							);
				}
			}
		}
	}

	@Test
	public void jar_fileScheme_specialCharacter() throws Exception {
		Path initialJarPath = createJar( root -> {
			addMetaInfFile( root );
			addSimpleClass( root );
		} );
		Path parentDirWithSpecialChar = temporaryFolder.newFolder().toPath()
				.resolve( "parentnamewith%40special@char" );
		Files.createDirectories( parentDirWithSpecialChar );
		Path jarPath = Files.copy(
				initialJarPath,
				parentDirWithSpecialChar.resolve( "namewith%40special@char.jar" )
		);

		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( jarPath.toUri().toURL() ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
			// Check that the JAR can be opened and that we can access other files within it
			try ( CodeSource codeSource = new CodeSource( location ) ) {
				try ( InputStream is = codeSource.readOrNull( META_INF_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).hasBinaryContent( META_INF_FILE_CONTENT );
				}
				try ( InputStream is = codeSource.readOrNull( NON_EXISTING_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).isNull();
				}
				Path classesPath = codeSource.classesPathOrFail();
				try ( Stream<Path> files = Files.walk( classesPath ).filter( Files::isRegularFile ) ) {
					assertThat( files )
							.containsExactlyInAnyOrder(
									classesPath.resolve( META_INF_FILE_RELATIVE_PATH ),
									classesPath.resolve( SIMPLE_CLASS_RELATIVE_PATH )
							);
				}
			}
		}
	}

	@Test
	public void directory_specialCharacter() throws Exception {
		Path initialDirPath = createDir( root -> {
			addMetaInfFile( root );
			addSimpleClass( root );
		} );
		Path parentDirWithSpecialChar = temporaryFolder.newFolder().toPath()
				.resolve( "parentnamewith%40special@char" );
		Files.createDirectories( parentDirWithSpecialChar );
		Path dirPath = Files.move(
				initialDirPath,
				parentDirWithSpecialChar.resolve( "namewith%40special@char.jar" )
		);

		try ( URLClassLoader isolatedClassLoader = createIsolatedClassLoader( dirPath.toUri().toURL() ) ) {
			Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( SimpleClass.class.getName() );

			URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
			// Check that the JAR can be opened and that we can access other files within it
			try ( CodeSource codeSource = new CodeSource( location ) ) {
				try ( InputStream is = codeSource.readOrNull( META_INF_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).hasBinaryContent( META_INF_FILE_CONTENT );
				}
				try ( InputStream is = codeSource.readOrNull( NON_EXISTING_FILE_RELATIVE_PATH ) ) {
					assertThat( is ).isNull();
				}
				Path classesPath = codeSource.classesPathOrFail();
				try ( Stream<Path> files = Files.walk( classesPath ).filter( Files::isRegularFile ) ) {
					assertThat( files )
							.containsExactlyInAnyOrder(
									classesPath.resolve( META_INF_FILE_RELATIVE_PATH ),
									classesPath.resolve( SIMPLE_CLASS_RELATIVE_PATH )
							);
				}
			}
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

	private void addMetaInfFile(Path root) throws IOException {
		Path file = root.resolve( "META-INF/someFile.txt" );
		Files.createDirectories( file.getParent() );
		try ( InputStream stream = new ByteArrayInputStream( META_INF_FILE_CONTENT ) ) {
			Files.copy( stream, file );
		}
	}

	private void addSimpleClass(Path classesDir) throws IOException {
		String classResourceName = SIMPLE_CLASS_RELATIVE_PATH;
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
