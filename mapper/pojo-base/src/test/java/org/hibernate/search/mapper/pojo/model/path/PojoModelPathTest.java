/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;

class PojoModelPathTest {

	@Test
	void ofProperty() {
		assertThat( PojoModelPath.ofProperty( "foo" ) )
				.satisfies( isPath( "foo" ) );

		assertThatThrownBy(
				() -> PojoModelPath.ofProperty( null )
		)
				.isInstanceOf( IllegalArgumentException.class );

		assertThatThrownBy(
				() -> PojoModelPath.ofProperty( "" )
		)
				.isInstanceOf( IllegalArgumentException.class );

		assertThatThrownBy(
				() -> PojoModelPath.ofProperty( "foo.bar" )
		)
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void ofValue_property() {
		assertThat( PojoModelPath.ofValue( "foo" ) )
				.satisfies( isPath( "foo", ContainerExtractorPath.defaultExtractors() ) );

		assertThatThrownBy(
				() -> PojoModelPath.ofValue( null )
		)
				.isInstanceOf( IllegalArgumentException.class );

		assertThatThrownBy(
				() -> PojoModelPath.ofValue( "" )
		)
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void ofValue_propertyAndContainerExtractorPath() {
		assertThat(
				PojoModelPath.ofValue( "foo", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) ) )
				.satisfies( isPath( "foo", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) ) );

		assertThatThrownBy(
				() -> PojoModelPath.ofValue( null,
						ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) )
		)
				.isInstanceOf( IllegalArgumentException.class );

		assertThatThrownBy(
				() -> PojoModelPath.ofValue( "",
						ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) )
		)
				.isInstanceOf( IllegalArgumentException.class );

		assertThatThrownBy(
				() -> PojoModelPath.ofValue( "foo", null )
		)
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void parse() {
		assertThat( PojoModelPath.parse( "foo" ) )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.defaultExtractors()
				) );

		assertThat( PojoModelPath.parse( "foo.bar" ) )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.defaultExtractors(),
						"bar", ContainerExtractorPath.defaultExtractors()
				) );

		assertThatThrownBy(
				() -> PojoModelPath.parse( null )
		)
				.isInstanceOf( IllegalArgumentException.class );

		assertThatThrownBy(
				() -> PojoModelPath.parse( "" )
		)
				.isInstanceOf( IllegalArgumentException.class );

		assertThatThrownBy(
				() -> PojoModelPath.parse( "foo..bar" )
		)
				.isInstanceOf( IllegalArgumentException.class );

		assertThatThrownBy(
				() -> PojoModelPath.parse( "foo." )
		)
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void builder() {
		PojoModelPath.Builder builder = PojoModelPath.builder();
		builder.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
				.property( "bar" ).valueWithoutExtractors()
				.property( "fubar" ).valueWithDefaultExtractors()
				.property( "other" ).value( BuiltinContainerExtractors.MAP_KEY )
				.property( "other2" ).value( ContainerExtractorPath.defaultExtractors() )
				.property( "other3" ).value( ContainerExtractorPath.noExtractors() )
				.property( "other4" ).value( ContainerExtractorPath.explicitExtractors( Arrays.asList(
						BuiltinContainerExtractors.ITERABLE, BuiltinContainerExtractors.OPTIONAL_DOUBLE
				) ) );
		assertThat( builder.toValuePath() )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
						"bar", ContainerExtractorPath.noExtractors(),
						"fubar", ContainerExtractorPath.defaultExtractors(),
						"other", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ),
						"other2", ContainerExtractorPath.defaultExtractors(),
						"other3", ContainerExtractorPath.noExtractors(),
						"other4", ContainerExtractorPath.explicitExtractors( Arrays.asList(
								BuiltinContainerExtractors.ITERABLE, BuiltinContainerExtractors.OPTIONAL_DOUBLE
						) )
				) );

		builder = PojoModelPath.builder();
		builder.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
				.property( "bar" ).value( BuiltinContainerExtractors.MAP_KEY );
		assertThat( builder.toPropertyPath() )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
						"bar"
				) );
	}

	@Test
	void builder_missingContainerExtractorPath_middle() {
		PojoModelPath.Builder builder = PojoModelPath.builder();
		builder.property( "foo" ).property( "bar" ).value( BuiltinContainerExtractors.MAP_KEY );
		assertThat( builder.toValuePath() )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.defaultExtractors(),
						"bar", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY )
				) );
	}

	@Test
	void builder_missingContainerExtractorPath_end() {
		PojoModelPath.Builder builder = PojoModelPath.builder();
		builder.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION ).property( "bar" );
		assertThat( builder.toValuePath() )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
						"bar", ContainerExtractorPath.defaultExtractors()
				) );

		builder = PojoModelPath.builder();
		builder.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION ).property( "bar" );
		assertThat( builder.toPropertyPath() )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.COLLECTION ),
						"bar"
				) );
	}

	@Test
	void builder_missingPropertyName() {
		assertThat( PojoModelPath.builder().toValuePathOrNull() ).isNull();

		assertThat( PojoModelPath.builder().toPropertyPathOrNull() ).isNull();

		String errorMessage = "A PojoModelPath must include at least one property";

		assertThatThrownBy(
				() -> PojoModelPath.builder().toValuePath()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		assertThatThrownBy(
				() -> PojoModelPath.builder().toPropertyPath()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		assertThatThrownBy(
				() -> PojoModelPath.builder().value( BuiltinContainerExtractors.COLLECTION )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		assertThatThrownBy(
				() -> PojoModelPath.builder().valueWithoutExtractors()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		assertThatThrownBy(
				() -> PojoModelPath.builder().valueWithDefaultExtractors()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );
	}

	@Test
	void builder_chainedContainerExtractors() {
		PojoModelPath.Builder builder = PojoModelPath.builder();
		builder.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION ).value( BuiltinContainerExtractors.ITERABLE )
				.property( "bar" ).value( BuiltinContainerExtractors.MAP_KEY );
		assertThat( builder.toValuePath() )
				.satisfies( isPath(
						"foo",
						ContainerExtractorPath.explicitExtractors( Arrays.asList(
								BuiltinContainerExtractors.COLLECTION, BuiltinContainerExtractors.ITERABLE
						) ),
						"bar", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY )
				) );
	}

	@Test
	void builder_chainedContainerExtractors_defaultExtractors() {
		assertThat( PojoModelPath.builder().property( "foo" )
				.valueWithoutExtractors().valueWithDefaultExtractors()
				.toValuePath()
		)
				.satisfies( isPath(
						"foo", ContainerExtractorPath.defaultExtractors()
				) );

		String errorMessage = "Invalid reference to default extractors:"
				+ " a chain of multiple container extractors must not include the default extractors";

		assertThatThrownBy(
				() -> PojoModelPath.builder().property( "foo" )
						.value( BuiltinContainerExtractors.COLLECTION ).valueWithDefaultExtractors()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		assertThatThrownBy(
				() -> PojoModelPath.builder().property( "foo" )
						.valueWithDefaultExtractors().value( BuiltinContainerExtractors.COLLECTION )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );
	}

	@Test
	void relativize_correctPrefix() {
		assertThat( PojoModelPath.builder()
				.property( "foo" )
				.property( "bar" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" )
						.toValuePath() ) )
				.hasValueSatisfying( isPath(
						"bar", ContainerExtractorPath.defaultExtractors()
				) );

		assertThat( PojoModelPath.builder()
				.property( "foo" )
				.property( "foobar" )
				.property( "bar" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" )
						.toValuePath() ) )
				.hasValueSatisfying( isPath(
						"foobar", ContainerExtractorPath.defaultExtractors(),
						"bar", ContainerExtractorPath.defaultExtractors()
				) );

		assertThat( PojoModelPath.builder()
				.property( "foo" )
				.property( "foobar" )
				.property( "bar" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" )
						.property( "foobar" )
						.toValuePath() ) )
				.hasValueSatisfying( isPath(
						"bar", ContainerExtractorPath.defaultExtractors()
				) );

		assertThat( PojoModelPath.builder()
				.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
				.property( "bar" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
						.toValuePath() ) )
				.hasValueSatisfying( isPath(
						"bar", ContainerExtractorPath.defaultExtractors()
				) );

		assertThat( PojoModelPath.builder()
				.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
				.property( "foobar" ).value( BuiltinContainerExtractors.MAP_KEY )
				.property( "bar" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
						.toValuePath() ) )
				.hasValueSatisfying( isPath(
						"foobar", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ),
						"bar", ContainerExtractorPath.defaultExtractors()
				) );

		assertThat( PojoModelPath.builder()
				.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
				.property( "foobar" ).value( BuiltinContainerExtractors.MAP_KEY )
				.property( "bar" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
						.property( "foobar" ).value( BuiltinContainerExtractors.MAP_KEY )
						.toValuePath() ) )
				.hasValueSatisfying( isPath(
						"bar", ContainerExtractorPath.defaultExtractors()
				) );
	}

	@Test
	void relativize_self() {
		assertThat( PojoModelPath.builder()
				.property( "foo" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" )
						.toValuePath() ) )
				.isEmpty();
		assertThat( PojoModelPath.builder()
				.property( "foo" ).valueWithoutExtractors()
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" ).valueWithoutExtractors()
						.toValuePath() ) )
				.isEmpty();
		assertThat( PojoModelPath.builder()
				.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
						.toValuePath() ) )
				.isEmpty();
		assertThat( PojoModelPath.builder()
				.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
				.property( "bar" ).valueWithoutExtractors()
				.property( "foobar" ).valueWithDefaultExtractors()
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" ).value( BuiltinContainerExtractors.COLLECTION )
						.property( "bar" ).valueWithoutExtractors()
						.property( "foobar" ).valueWithDefaultExtractors()
						.toValuePath() ) )
				.isEmpty();
	}

	@Test
	void relativize_unrelated() {
		assertThat( PojoModelPath.builder()
				.property( "foo" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "bar" )
						.property( "foobar" )
						.toValuePath() ) )
				.isEmpty();
		assertThat( PojoModelPath.builder()
				.property( "foo" )
				.property( "bar" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foobar" )
						.toValuePath() ) )
				.isEmpty();
		assertThat( PojoModelPath.builder()
				.property( "foo" )
				.property( "bar" )
				.property( "foobar" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" )
						.property( "foobar" )
						.toValuePath() ) )
				.isEmpty();
		assertThat( PojoModelPath.builder()
				.property( "foo" ).valueWithDefaultExtractors()
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "foo" ).valueWithoutExtractors()
						.property( "bar" )
						.toValuePath() ) )
				.isEmpty();
		assertThat( PojoModelPath.builder()
				.property( "foo" )
				.toValuePath()
				.relativize( PojoModelPath.builder()
						.property( "bar" )
						.property( "foo" )
						.toValuePath() ) )
				.isEmpty();
	}

	private static <T extends PojoModelPath> Consumer<T> isPath(Object... pathComponents) {
		return path -> {
			Deque<Object> components = new ArrayDeque<>();
			PojoModelPath currentPath = path;
			do {
				if ( currentPath instanceof PojoModelPathValueNode ) {
					components.addFirst( ( (PojoModelPathValueNode) currentPath ).extractorPath() );
				}
				else {
					components.addFirst( ( (PojoModelPathPropertyNode) currentPath ).propertyName() );
				}
				currentPath = currentPath.parent();
			}
			while ( currentPath != null );

			assertThat( components ).containsExactly( pathComponents );
		};
	}

}
