/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.jar.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.jar.JarTestUtils.toDirectory;
import static org.hibernate.search.util.impl.test.jar.JarTestUtils.toJar;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarFile;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.test.HibernateSearchUtilInternalTestCommonClass;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

class JandexUtilsTest {
	private static final String META_INF_JANDEX_INDEX = "META-INF/jandex.idx";

	private static final URL JUNIT_JAR_URL = JarUtils.codeSourceLocation( Test.class )
			.orElseThrow( () -> new AssertionFailure( "Could not find JUnit JAR?" ) );
	private static final URL UTIL_INTERNAL_TEST_COMMON_JAR_URL =
			JarUtils.codeSourceLocation( HibernateSearchUtilInternalTestCommonClass.class )
					.orElseThrow( () -> new AssertionFailure(
							"Could not find hibernate-search-util-internal-test-common JAR?" ) );

	@TempDir
	public Path temporaryFolder;

	@Test
	void readIndex_fromJar_indexAbsent() throws Exception {
		Path jarPath = toJar( temporaryFolder, JUNIT_JAR_URL );
		checkJarPreconditions( jarPath, false );

		Optional<Index> indexOptional = JandexUtils.readIndex( jarPath.toUri().toURL() );
		assertThat( indexOptional ).isEmpty();
	}

	@Test
	void readIndex_fromDirectory_indexAbsent() throws Exception {
		Path dirPath = toDirectory( temporaryFolder, JUNIT_JAR_URL );
		checkDirectoryPreconditions( dirPath, false );

		Optional<Index> indexOptional = JandexUtils.readIndex( dirPath.toUri().toURL() );
		assertThat( indexOptional ).isEmpty();
	}

	@Test
	void readIndex_fromJar_indexPresent() throws Exception {
		Path jarPath = toJar( temporaryFolder, UTIL_INTERNAL_TEST_COMMON_JAR_URL );
		checkJarPreconditions( jarPath, true );

		Optional<Index> indexOptional = JandexUtils.readIndex( jarPath.toUri().toURL() );
		assertThat( indexOptional ).isNotEmpty();
		Index index = indexOptional.get();
		ClassInfo someClassInfo = index.getClassByName(
				DotName.createSimple( HibernateSearchUtilInternalTestCommonClass.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( HibernateSearchUtilInternalTestCommonClass.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	void readIndex_fromDirectory_indexPresent() throws Exception {
		Path dirPath = toDirectory( temporaryFolder, UTIL_INTERNAL_TEST_COMMON_JAR_URL );
		checkDirectoryPreconditions( dirPath, true );

		Optional<Index> indexOptional = JandexUtils.readIndex( dirPath.toUri().toURL() );
		assertThat( indexOptional ).isNotEmpty();
		Index index = indexOptional.get();
		ClassInfo someClassInfo = index.getClassByName(
				DotName.createSimple( HibernateSearchUtilInternalTestCommonClass.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( HibernateSearchUtilInternalTestCommonClass.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	void readOrBuildIndex_fromJar_indexAbsent() throws Exception {
		Path jarPath = toJar( temporaryFolder, JUNIT_JAR_URL );
		checkJarPreconditions( jarPath, false );

		Index index = JandexUtils.readOrBuildIndex( jarPath.toUri().toURL() );
		ClassInfo someClassInfo = index.getClassByName( DotName.createSimple( Test.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( Test.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	void readOrBuildIndex_fromDirectory_indexAbsent() throws Exception {
		Path dirPath = toDirectory( temporaryFolder, JUNIT_JAR_URL );
		checkDirectoryPreconditions( dirPath, false );

		Index index = JandexUtils.readOrBuildIndex( dirPath.toUri().toURL() );
		ClassInfo someClassInfo = index.getClassByName( DotName.createSimple( Test.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( Test.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	void readOrBuildIndex_fromJar_indexPresent() throws Exception {
		Path jarPath = toJar( temporaryFolder, UTIL_INTERNAL_TEST_COMMON_JAR_URL );
		checkJarPreconditions( jarPath, true );

		Index index = JandexUtils.readOrBuildIndex( jarPath.toUri().toURL() );
		ClassInfo someClassInfo = index.getClassByName(
				DotName.createSimple( HibernateSearchUtilInternalTestCommonClass.class.getName() ) );
		assertThat( someClassInfo ).isNotNull()
				.returns( HibernateSearchUtilInternalTestCommonClass.class.getName(), ci -> ci.name().toString() );
	}

	@Test
	void readOrBuildIndex_fromDirectory_indexPresent() throws Exception {
		Path dirPath = toDirectory( temporaryFolder, UTIL_INTERNAL_TEST_COMMON_JAR_URL );
		checkDirectoryPreconditions( dirPath, true );

		Index index = JandexUtils.readOrBuildIndex( dirPath.toUri().toURL() );
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
