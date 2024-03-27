/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.repackaged.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.util.common.jar.impl.JandexUtils;
import org.hibernate.search.util.impl.test.SystemHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;

import acme.org.hibernate.search.integrationtest.spring.repackaged.model.MyEntity;
import acme.org.hibernate.search.integrationtest.spring.repackaged.model.MyProjection;
import org.springframework.boot.loader.net.protocol.Handlers;
import org.springframework.boot.loader.net.protocol.jar.JarUrl;

/**
 * This test is NOT a @SpringBootTest:
 * it manipulates the repackaged JAR
 * to inspect it and run it in a separate JVM.
 */
class RepackagedApplicationIT {
	private String javaLauncherCommand;
	private Path repackedJarPath;

	@BeforeEach
	void init() {
		javaLauncherCommand = System.getProperty( "test.launcher-command" );
		String repackedJarPathString = System.getProperty( "test.repackaged-jar-path" );
		repackedJarPath = repackedJarPathString == null ? null : Paths.get( repackedJarPathString );

		if ( javaLauncherCommand == null
				|| repackedJarPath == null
				|| !Files.exists( repackedJarPath ) ) {
			throw new IllegalStateException( "Tests seem to be running from an IDE."
					+ " This test cannot run from the IDE"
					+ " as it requires the application to get repackaged through a Maven plugin" );
		}
	}

	/**
	 * Test makes sure that the (repackaged) application behaves as expected on the current JDK.
	 */
	@Test
	void smokeTest() throws Exception {
		Process process = SystemHelper.runCommandWithInheritedIO( javaLauncherCommand, "-jar", repackedJarPath.toString() );
		assertThat( process.exitValue() )
				.as( "Starting the repackaged application should succeed" )
				.isEqualTo( 0 );
	}

	/**
	 * Test makes sure that our utils can read the classes from the nested jar inside a Spring's repackaged uberjar.
	 * We try to create index and then see that that index has the info we need e.g. projection/entity classes etc.
	 */
	@Test
	void canReadJar() throws Exception {

		try ( JarFile outerJar = new JarFile( repackedJarPath.toFile() ) ) {
			for ( JarEntry jarEntry : outerJar.stream().toList() ) {
				if ( jarEntry.getName().contains( "hibernate-search-integrationtest-spring-repackaged-model" ) ) {
					URL innerJarURL = innerJarUrl( jarEntry );

					try ( URLClassLoader isolatedClassLoader = new URLClassLoader( new URL[] { innerJarURL }, null ) ) {
						Class<?> classInIsolatedClassLoader = isolatedClassLoader.loadClass( MyEntity.class.getName() );
						URL location = classInIsolatedClassLoader.getProtectionDomain().getCodeSource().getLocation();
						assertThat( location.getProtocol() ).isEqualTo( "jar" );
						Index index = JandexUtils.readOrBuildIndex( location );

						ClassInfo classInfo = index.getClassByName( DotName.createSimple( MyEntity.class.getName() ) );
						assertThat( classInfo ).isNotNull();
						FieldInfo name = classInfo.field( "name" );
						assertThat( name ).isNotNull();
						assertThat( name.type().name().toString() ).isEqualTo( String.class.getName() );

						classInfo = index.getClassByName( DotName.createSimple( MyProjection.class.getName() ) );
						assertThat( classInfo ).isNotNull();
						assertThat(
								classInfo.annotation( DotName.createSimple( ProjectionConstructor.class.getName() ) ) )
								.isNotNull();
						name = classInfo.field( "name" );
						assertThat( name ).isNotNull();
						assertThat( name.type().name().toString() ).isEqualTo( String.class.getName() );
					}
				}
			}
		}
	}

	URL innerJarUrl(JarEntry jarEntry) {
		Handlers.register();
		return JarUrl.create( repackedJarPath.toFile(), jarEntry );
	}
}
