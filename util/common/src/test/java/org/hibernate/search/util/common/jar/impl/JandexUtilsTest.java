/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.jar.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.common.jar.impl.JarUtils.jarOrDirectoryPath;
import static org.hibernate.search.util.impl.test.jar.JarTestUtils.toDirectory;
import static org.hibernate.search.util.impl.test.jar.JarTestUtils.toJar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarFile;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.test.HibernateSearchUtilInternalTestCommonClass;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

public class JandexUtilsTest {
	private static final String META_INF_JANDEX_INDEX = "META-INF/jandex.idx";

	private static final Path PATH_TO_JUNIT_JAR = jarOrDirectoryPath( Test.class )
			.orElseThrow( () -> new AssertionFailure( "Could not find JUnit JAR?" ) );
	private static final Path PATH_TO_UTIL_INTERNAL_TEST_COMMON_JAR =
			jarOrDirectoryPath( HibernateSearchUtilInternalTestCommonClass.class )
					.orElseThrow( () -> new AssertionFailure(
							"Could not find hibernate-search-util-internal-test-common JAR?" ) );

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void readIndex_fromJar_indexAbsent() {
		Path jarPath = toJar( temporaryFolder, PATH_TO_JUNIT_JAR );
		checkJarPreconditions( jarPath, false );

		Optional<Index> indexOptional = JandexUtils.readIndex( jarPath );
		assertThat( indexOptional ).isEmpty();
	}

	@Test
	public void readIndex_fromDirectory_indexAbsent() {
		Path dirPath = toDirectory( temporaryFolder, PATH_TO_JUNIT_JAR );
		checkDirectoryPreconditions( dirPath, false );

		Optional<Index> indexOptional = JandexUtils.readIndex( dirPath );
		assertThat( indexOptional ).isEmpty();
	}

	@Test
	public void readIndex_fromJar_indexPresent() {
		Path jarPath = toJar( temporaryFolder, PATH_TO_UTIL_INTERNAL_TEST_COMMON_JAR );
		checkJarPreconditions( jarPath, true );

		Optional<Index> indexOptional = JandexUtils.readIndex( jarPath );
		assertThat( indexOptional ).isNotEmpty();
		Index index = indexOptional.get();
		ClassInfo someClassInfo = index.getClassByName(
				DotName.createSimple( HibernateSearchUtilInternalTestCommonClass.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( HibernateSearchUtilInternalTestCommonClass.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	public void readIndex_fromDirectory_indexPresent() {
		Path dirPath = toDirectory( temporaryFolder, PATH_TO_UTIL_INTERNAL_TEST_COMMON_JAR );
		checkDirectoryPreconditions( dirPath, true );

		Optional<Index> indexOptional = JandexUtils.readIndex( dirPath );
		assertThat( indexOptional ).isNotEmpty();
		Index index = indexOptional.get();
		ClassInfo someClassInfo = index.getClassByName(
				DotName.createSimple( HibernateSearchUtilInternalTestCommonClass.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( HibernateSearchUtilInternalTestCommonClass.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	public void readOrBuildIndex_fromJar_indexAbsent() {
		Path jarPath = toJar( temporaryFolder, PATH_TO_JUNIT_JAR );
		checkJarPreconditions( jarPath, false );

		Index index = JandexUtils.readOrBuildIndex( jarPath );
		ClassInfo someClassInfo = index.getClassByName( DotName.createSimple( Test.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( Test.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	public void readOrBuildIndex_fromDirectory_indexAbsent() {
		Path dirPath = toDirectory( temporaryFolder, PATH_TO_JUNIT_JAR );
		checkDirectoryPreconditions( dirPath, false );

		Index index = JandexUtils.readOrBuildIndex( dirPath );
		ClassInfo someClassInfo = index.getClassByName( DotName.createSimple( Test.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( Test.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	public void readOrBuildIndex_fromJar_indexPresent() {
		Path jarPath = toJar( temporaryFolder, PATH_TO_UTIL_INTERNAL_TEST_COMMON_JAR );
		checkJarPreconditions( jarPath, true );

		Index index = JandexUtils.readOrBuildIndex( jarPath );
		ClassInfo someClassInfo = index.getClassByName(
				DotName.createSimple( HibernateSearchUtilInternalTestCommonClass.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( HibernateSearchUtilInternalTestCommonClass.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	public void readOrBuildIndex_fromDirectory_indexPresent() {
		Path dirPath = toDirectory( temporaryFolder, PATH_TO_UTIL_INTERNAL_TEST_COMMON_JAR );
		checkDirectoryPreconditions( dirPath, true );

		Index index = JandexUtils.readOrBuildIndex( dirPath );
		ClassInfo someClassInfo = index.getClassByName(
				DotName.createSimple( HibernateSearchUtilInternalTestCommonClass.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( HibernateSearchUtilInternalTestCommonClass.class.getName(), ci -> ci.name().toString() );
	}

	private void checkJarPreconditions(Path path, boolean expectedIndexPresent) {
		if ( !Files.isRegularFile( path ) ) {
			throw new AssertionFailure( "Code source at path " + path + " is not a JAR file as expected."
					+ " There is a bug in the tests." );
		}
		try ( JarFile jarFile = new JarFile( path.toFile() ) ) {
			boolean actualIndexPresent = jarFile.getEntry( META_INF_JANDEX_INDEX ) != null;
			if ( actualIndexPresent != expectedIndexPresent ) {
				throw new IllegalStateException( "Code source at path " + path + " index content is unexpected."
						+ " Expected index present: " + expectedIndexPresent + "; actual index present: " + actualIndexPresent
						+ ". This might be caused by IDE limitations;"
						+ " try running this test from Maven instead." );
			}
		}
		catch (Exception e) {
			throw new RuntimeException( e.getMessage(), e );
		}
	}

	private void checkDirectoryPreconditions(Path path, boolean expectedIndexPresent) {
		if ( !Files.isDirectory( path ) ) {
			throw new AssertionFailure( "Code source at path " + path + " is not a directory as expected."
					+ " There is a bug in the tests." );
		}
		boolean actualIndexPresent = Files.exists( path.resolve( META_INF_JANDEX_INDEX ) );
		if ( actualIndexPresent != expectedIndexPresent ) {
			throw new IllegalStateException( "Code source at path " + path + " index content is unexpected."
					+ " Expected index present: " + expectedIndexPresent + "; actual index present: " + actualIndexPresent
					+ ". This might be caused by IDE limitations;"
					+ " try running this test from Maven instead." );
		}
	}
}