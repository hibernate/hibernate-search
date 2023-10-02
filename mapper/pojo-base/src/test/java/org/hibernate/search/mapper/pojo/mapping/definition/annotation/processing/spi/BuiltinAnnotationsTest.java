/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.common.jar.impl.JandexUtils.readOrBuildIndex;
import static org.hibernate.search.util.common.jar.impl.JarUtils.codeSourceLocation;

import java.io.IOException;
import java.util.Set;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.jar.impl.JandexUtils;
import org.hibernate.search.util.impl.test.jar.JandexTestUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

class BuiltinAnnotationsTest {

	private static Index pojoMapperBaseIndex;

	@BeforeAll
	static void index() throws IOException {
		pojoMapperBaseIndex = readOrBuildIndex(
				codeSourceLocation( BuiltinAnnotations.class )
						.orElseThrow( () -> new AssertionFailure( "Could not find hibernate-search-mapper-pojo-base JAR?" ) )
		);
	}

	@Test
	void testRootMapping() {
		assertThat( pojoMapperBaseIndex.getClassByName( BuiltinAnnotations.ROOT_MAPPING ) ).isNotNull();
	}

	@Test
	void testNoMissingRootMappingAnnotation() {
		Set<DotName> rootMappingAnnotatedAnnotations = JandexUtils.findAnnotatedAnnotationsAndContaining(
				pojoMapperBaseIndex, BuiltinAnnotations.ROOT_MAPPING );

		Set<String> rootMappingAnnotatedAnnotationsAsStrings =
				JandexTestUtils.toStrings( rootMappingAnnotatedAnnotations );

		assertThat( rootMappingAnnotatedAnnotationsAsStrings ).isNotEmpty();
		assertThat( JandexTestUtils.toStrings( BuiltinAnnotations.ROOT_MAPPING_ANNOTATIONS ) )
				.containsExactlyInAnyOrderElementsOf( rootMappingAnnotatedAnnotationsAsStrings );
	}
}
