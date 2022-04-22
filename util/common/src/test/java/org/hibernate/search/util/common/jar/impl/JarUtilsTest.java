/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.jar.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.jar.JarTestUtils.toJar;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JarUtilsTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void openJarOrDirectory_directory() throws Exception {
		Path dirPath = temporaryFolder.newFolder().toPath();
		try ( FileSystem jarFs = JarUtils.openJarOrDirectory( dirPath ) ) {
			assertThat( jarFs ).isNull();
		}
	}

	@Test
	public void openJarOrDirectory_jar() throws Exception {
		Path dirPath = temporaryFolder.newFolder().toPath();
		Files.createFile( dirPath.resolve( "someFile.txt" ) );
		Path jarPath = toJar( temporaryFolder, dirPath );

		try ( FileSystem jarFs = JarUtils.openJarOrDirectory( jarPath ) ) {
			assertThat( jarFs ).isNotNull();
			Path jarRoot = jarFs.getRootDirectories().iterator().next();
			assertThat( Files.exists( jarRoot.resolve( "someFile.txt" ) ) ).isTrue();
		}
	}

	@Test
	public void openJarOrDirectory_jar_specialCharacter() throws Exception {
		Path dirPath = temporaryFolder.newFolder().toPath();
		Files.createFile( dirPath.resolve( "someFile.txt" ) );
		Path initialJarPath = toJar( temporaryFolder, dirPath );
		Path jarPath = Files.copy( initialJarPath,
				temporaryFolder.newFolder().toPath().resolve( "namewith@specialchar.jar" ) );

		try ( FileSystem jarFs = JarUtils.openJarOrDirectory( jarPath ) ) {
			assertThat( jarFs ).isNotNull();
			Path jarRoot = jarFs.getRootDirectories().iterator().next();
			assertThat( Files.exists( jarRoot.resolve( "someFile.txt" ) ) ).isTrue();
		}
	}

}
