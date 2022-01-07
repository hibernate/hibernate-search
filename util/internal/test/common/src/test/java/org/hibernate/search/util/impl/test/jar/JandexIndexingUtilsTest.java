/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.jar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.jar.JandexIndexingUtils.indexJarOrDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

public class JandexIndexingUtilsTest {

	private static final Path PATH_TO_JUNIT_JAR = JarUtils.determineJarOrDirectoryLocation( Test.class, "JUnit" );

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void indexJar() throws IOException {
		Index index = indexJarOrDirectory( PATH_TO_JUNIT_JAR );
		ClassInfo junitTestAnnotationClassInfo = index.getClassByName( DotName.createSimple( Test.class.getName() ) );
		assertThat( junitTestAnnotationClassInfo ).isNotNull()
				.returns( Test.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	public void indexJar_specialCharacter() throws IOException {
		Path tempDir = temporaryFolder.newFolder().toPath();
		Path jarCopy = Files.copy( PATH_TO_JUNIT_JAR, tempDir.resolve( PATH_TO_JUNIT_JAR.getFileName() ) );
		Files.move( jarCopy, jarCopy.resolveSibling( "namewith@specialchar.jar" ) );

		Index index = indexJarOrDirectory( PATH_TO_JUNIT_JAR );
		ClassInfo junitTestAnnotationClassInfo = index.getClassByName( DotName.createSimple( Test.class.getName() ) );
		assertThat( junitTestAnnotationClassInfo ).isNotNull()
				.returns( Test.class.getName(), ci -> ci.name().toString() );
	}

}