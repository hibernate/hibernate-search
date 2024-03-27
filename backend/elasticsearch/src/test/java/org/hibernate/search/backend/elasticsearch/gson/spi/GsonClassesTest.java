/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.common.jar.impl.JandexUtils.extractDeclaringClass;
import static org.hibernate.search.util.common.jar.impl.JandexUtils.readOrBuildIndex;
import static org.hibernate.search.util.common.jar.impl.JarUtils.codeSourceLocation;
import static org.hibernate.search.util.impl.test.jar.JandexTestUtils.findRuntimeAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.test.jar.JandexTestUtils;
import org.hibernate.search.util.impl.test.logging.Log;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;

class GsonClassesTest {

	private static Index gsonIndex;
	private static Index backendElasticsearchIndex;

	@BeforeAll
	static void index() throws IOException {
		gsonIndex = readOrBuildIndex( codeSourceLocation( Gson.class )
				.orElseThrow( () -> new AssertionFailure( "Could not find Gson JAR?" ) ) );
		backendElasticsearchIndex = readOrBuildIndex( codeSourceLocation( ElasticsearchExtension.class )
				.orElseThrow( () -> new AssertionFailure( "Could not find hibernate-search-backend-elasticsearch JAR?" ) ) );
	}

	@Test
	void testNoMissingGsonAnnotatedClass() {
		Set<DotName> gsonAnnotations = findRuntimeAnnotations( gsonIndex );

		Set<DotName> annotatedClasses = new HashSet<>();
		for ( DotName gsonAnnotation : gsonAnnotations ) {
			for ( AnnotationInstance annotationInstance : backendElasticsearchIndex.getAnnotations( gsonAnnotation ) ) {
				DotName className = extractDeclaringClass( annotationInstance.target() ).name();
				annotatedClasses.add( className );
			}
		}

		Set<String> annotatedClassesAndSubclasses = JandexTestUtils.toStrings(
				JandexTestUtils.collectClassHierarchiesRecursively( backendElasticsearchIndex, annotatedClasses ) );

		Log.INSTANCE.infof( "GSON-annotated classes and their class hierarchy: %s", annotatedClassesAndSubclasses );
		assertThat( annotatedClassesAndSubclasses ).isNotEmpty();
		assertThat( GsonClasses.typesRequiringReflection() ).containsAll( annotatedClassesAndSubclasses );
	}

	@Test
	void testNoMissingGsonContractImplementations() {
		List<DotName> gsonContracts = Arrays.asList(
				DotName.createSimple( TypeAdapterFactory.class.getName() ),
				DotName.createSimple( TypeAdapter.class.getName() )
		);

		Set<DotName> classes = new HashSet<>();
		for ( DotName gsonContract : gsonContracts ) {
			for ( ClassInfo implementor : backendElasticsearchIndex.getAllKnownSubclasses( gsonContract ) ) {
				classes.add( implementor.name() );
			}
			for ( ClassInfo implementor : backendElasticsearchIndex.getAllKnownImplementors( gsonContract ) ) {
				classes.add( implementor.name() );
			}
		}

		Set<String> classesAndSubclasses = JandexTestUtils.toStrings(
				JandexTestUtils.collectClassHierarchiesRecursively( backendElasticsearchIndex, classes ) );

		Log.INSTANCE.infof( "Gson contract implementations and their class hierarchy: %s", classesAndSubclasses );
		assertThat( classesAndSubclasses ).isNotEmpty();
		assertThat( GsonClasses.typesRequiringReflection() ).containsAll( classesAndSubclasses );
	}

}
