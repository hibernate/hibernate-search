/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.spi.ContainerExtractorRegistry;
import org.hibernate.search.mapper.pojo.model.typepattern.impl.TypePatternMatcherFactory;
import org.hibernate.search.mapper.pojo.testsupport.TestBeanResolver;
import org.hibernate.search.mapper.pojo.testsupport.TestIntrospector;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.assertj.core.api.InstanceOfAssertFactories;

class ContainerExtractorBinderDefaultExtractorTest {

	static List<Arguments> defaultExtractorMatches() {
		return List.of(
				Arguments.of( new TypeCapture<String>() {}, Collections.emptyList() ),
				Arguments.of( new TypeCapture<String[]>() {},
						Collections.singletonList( BuiltinContainerExtractors.ARRAY_OBJECT ) ),
				Arguments.of( new TypeCapture<char[]>() {},
						Collections.singletonList( BuiltinContainerExtractors.ARRAY_CHAR ) ),
				Arguments.of( new TypeCapture<boolean[]>() {},
						Collections.singletonList( BuiltinContainerExtractors.ARRAY_BOOLEAN ) ),
				Arguments.of( new TypeCapture<byte[]>() {},
						Collections.singletonList( BuiltinContainerExtractors.ARRAY_BYTE ) ),
				Arguments.of( new TypeCapture<short[]>() {},
						Collections.singletonList( BuiltinContainerExtractors.ARRAY_SHORT ) ),
				Arguments.of( new TypeCapture<int[]>() {}, Collections.singletonList( BuiltinContainerExtractors.ARRAY_INT ) ),
				Arguments.of( new TypeCapture<long[]>() {},
						Collections.singletonList( BuiltinContainerExtractors.ARRAY_LONG ) ),
				Arguments.of( new TypeCapture<float[]>() {},
						Collections.singletonList( BuiltinContainerExtractors.ARRAY_FLOAT ) ),
				Arguments.of( new TypeCapture<double[]>() {},
						Collections.singletonList( BuiltinContainerExtractors.ARRAY_DOUBLE ) ),
				Arguments.of( new TypeCapture<Iterable<String>>() {},
						Collections.singletonList( BuiltinContainerExtractors.ITERABLE ) ),
				Arguments.of( new TypeCapture<List<String>>() {},
						Collections.singletonList( BuiltinContainerExtractors.COLLECTION ) ),
				Arguments.of(
						new TypeCapture<Map<String, String>>() {},
						Collections.singletonList( BuiltinContainerExtractors.MAP_VALUE ) ),
				Arguments.of(
						new TypeCapture<List<Map<String, Integer>>>() {},
						Arrays.asList( BuiltinContainerExtractors.COLLECTION, BuiltinContainerExtractors.MAP_VALUE ) ),
				Arguments.of(
						new TypeCapture<List<String[]>>() {},
						Arrays.asList( BuiltinContainerExtractors.COLLECTION, BuiltinContainerExtractors.ARRAY_OBJECT ) ),
				Arguments.of(
						new TypeCapture<List<int[]>>() {},
						Arrays.asList( BuiltinContainerExtractors.COLLECTION, BuiltinContainerExtractors.ARRAY_INT ) ),
				Arguments.of(
						new TypeCapture<List<String>[]>() {},
						Arrays.asList( BuiltinContainerExtractors.ARRAY_OBJECT, BuiltinContainerExtractors.COLLECTION ) )
		);
	}

	@SuppressWarnings("unchecked")
	static List<Arguments> defaultExtractorMismatches() {
		List<Arguments> result = new ArrayList<>();
		for ( Arguments match : defaultExtractorMatches() ) {
			for ( List<String> mismatchingExtractors : matchingToMismatching( (List<String>) match.get()[1] ) ) {
				result.add( Arguments.of( match.get()[0], mismatchingExtractors ) );
			}
		}
		return result;
	}

	private static List<List<String>> matchingToMismatching(List<String> matchingExtractorNames) {
		List<List<String>> nonMatching = new ArrayList<>();
		if ( !matchingExtractorNames.isEmpty() ) {
			for ( int i = 0; i < ( matchingExtractorNames.size() - 1 ); i++ ) {
				List<String> tooShort = matchingExtractorNames.subList( 0, i );
				nonMatching.add( tooShort );
			}
			List<String> firstWrong = new ArrayList<>( matchingExtractorNames );
			firstWrong.set( 0,
					BuiltinContainerExtractors.COLLECTION.equals( matchingExtractorNames.get( 0 ) )
							? BuiltinContainerExtractors.ITERABLE
							: BuiltinContainerExtractors.COLLECTION );
			nonMatching.add( firstWrong );
		}
		for ( String extraName : new String[] {
				BuiltinContainerExtractors.OPTIONAL,
				BuiltinContainerExtractors.COLLECTION,
				BuiltinContainerExtractors.ARRAY_INT
		} ) {
			List<String> tooLong = new ArrayList<>( matchingExtractorNames );
			tooLong.add( extraName );
			nonMatching.add( tooLong );
		}
		return nonMatching;
	}

	private final TestIntrospector introspector =
			new TestIntrospector( ValueHandleFactory.usingMethodHandle( MethodHandles.lookup() ) );
	private ContainerExtractorBinder binder;

	@BeforeEach
	void init() {
		binder = new ContainerExtractorBinder( new TestBeanResolver(),
				ContainerExtractorRegistry.builder().build(),
				new TypePatternMatcherFactory( introspector ) );
	}

	@ParameterizedTest
	@MethodSource("defaultExtractorMatches")
	void bindPath(TypeCapture<?> typeCapture, List<String> expectedExtractorNames) {
		assertThat( binder.bindPath(
				introspector.typeModel( typeCapture ),
				ContainerExtractorPath.defaultExtractors()
		) )
				.extracting( BoundContainerExtractorPath::getExtractorPath )
				.extracting(
						ContainerExtractorPath::explicitExtractorNames, InstanceOfAssertFactories.list( String.class ) )
				.containsExactlyElementsOf( expectedExtractorNames );
	}

	@ParameterizedTest
	@MethodSource("defaultExtractorMatches")
	void tryBindPath(TypeCapture<?> typeCapture, List<String> expectedExtractorNames) {
		assertThat( binder.tryBindPath( introspector.typeModel( typeCapture ),
				ContainerExtractorPath.defaultExtractors() ) )
				.hasValueSatisfying( v -> assertThat( v )
						.extracting( BoundContainerExtractorPath::getExtractorPath )
						.extracting( ContainerExtractorPath::explicitExtractorNames,
								InstanceOfAssertFactories.list( String.class ) )
						.containsExactlyElementsOf( expectedExtractorNames ) );
	}

	@ParameterizedTest
	@MethodSource("defaultExtractorMatches")
	void isDefaultExtractor_match(TypeCapture<?> typeCapture, List<String> matchingExtractorNames) {
		assertThat( binder.isDefaultExtractorPath( introspector.typeModel( typeCapture ),
				ContainerExtractorPath.explicitExtractors( matchingExtractorNames )
		) ).isTrue();
	}

	@ParameterizedTest
	@MethodSource("defaultExtractorMismatches")
	void isDefaultExtractor_mismatch(TypeCapture<?> typeCapture, List<String> mismatchingExtractorNames) {
		assertThat( binder.isDefaultExtractorPath( introspector.typeModel( typeCapture ),
				ContainerExtractorPath.explicitExtractors( mismatchingExtractorNames ) ) )
				.isFalse();
	}

}
