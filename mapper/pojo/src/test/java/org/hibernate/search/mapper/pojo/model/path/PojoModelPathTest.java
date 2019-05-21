/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.Consumer;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

public class PojoModelPathTest {

	@Test
	public void ofProperty() {
		assertThat( PojoModelPath.ofProperty( "foo" ) )
				.satisfies( isPath( "foo" ) );

		SubTest.expectException(
				() -> PojoModelPath.ofProperty( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );

		SubTest.expectException(
				() -> PojoModelPath.ofProperty( "" )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	public void ofValue_property() {
		assertThat( PojoModelPath.ofValue( "foo" ) )
				.satisfies( isPath( "foo", ContainerExtractorPath.defaultExtractors() ) );

		SubTest.expectException(
				() -> PojoModelPath.ofValue( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );

		SubTest.expectException(
				() -> PojoModelPath.ofValue( "" )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	public void ofValue_propertyAndContainerExtractorPath() {
		assertThat( PojoModelPath.ofValue( "foo", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) ) )
				.satisfies( isPath( "foo", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) ) );

		SubTest.expectException(
				() -> PojoModelPath.ofValue( null, ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );

		SubTest.expectException(
				() -> PojoModelPath.ofValue( "", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ) )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );

		SubTest.expectException(
				() -> PojoModelPath.ofValue( "foo", null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	public void parse() {
		assertThat( PojoModelPath.parse( "foo" ) )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.defaultExtractors()
				) );

		assertThat( PojoModelPath.parse( "foo.bar" ) )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.defaultExtractors(),
						"bar", ContainerExtractorPath.defaultExtractors()
				) );

		SubTest.expectException(
				() -> PojoModelPath.parse( null )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );

		SubTest.expectException(
				() -> PojoModelPath.parse( "" )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );

		SubTest.expectException(
				() -> PojoModelPath.parse( "foo..bar" )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );

		SubTest.expectException(
				() -> PojoModelPath.parse( "foo." )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	public void builder() {
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
	public void builder_missingContainerExtractorPath_middle() {
		PojoModelPath.Builder builder = PojoModelPath.builder();
		builder.property( "foo" ).property( "bar" ).value( BuiltinContainerExtractors.MAP_KEY );
		assertThat( builder.toValuePath() )
				.satisfies( isPath(
						"foo", ContainerExtractorPath.defaultExtractors(),
						"bar", ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY )
				) );
	}

	@Test
	public void builder_missingContainerExtractorPath_end() {
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
	public void builder_missingPropertyName() {
		assertThat( PojoModelPath.builder().toValuePathOrNull() ).isNull();

		assertThat( PojoModelPath.builder().toPropertyPathOrNull() ).isNull();

		String errorMessage = "A PojoModelPath must include at least one property";

		SubTest.expectException(
				() -> PojoModelPath.builder().toValuePath()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		SubTest.expectException(
				() -> PojoModelPath.builder().toPropertyPath()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		SubTest.expectException(
				() -> PojoModelPath.builder().value( BuiltinContainerExtractors.COLLECTION )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		SubTest.expectException(
				() -> PojoModelPath.builder().valueWithoutExtractors()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		SubTest.expectException(
				() -> PojoModelPath.builder().valueWithDefaultExtractors()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );
	}

	@Test
	public void builder_chainedContainerExtractors() {
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
	public void builder_chainedContainerExtractors_defaultExtractors() {
		assertThat( PojoModelPath.builder().property( "foo" )
				.valueWithoutExtractors().valueWithDefaultExtractors()
				.toValuePath()
		)
				.satisfies( isPath(
						"foo", ContainerExtractorPath.defaultExtractors()
				) );

		String errorMessage = "chain of multiple container extractors cannot include the default extractors";

		SubTest.expectException(
				() -> PojoModelPath.builder().property( "foo" )
						.value( BuiltinContainerExtractors.COLLECTION ).valueWithDefaultExtractors()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );

		SubTest.expectException(
				() -> PojoModelPath.builder().property( "foo" )
						.valueWithDefaultExtractors().value( BuiltinContainerExtractors.COLLECTION )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( errorMessage );
	}

	private static <T extends PojoModelPath> Consumer<T> isPath(Object ... pathComponents) {
		return path -> {
			Deque<Object> components = new ArrayDeque<>();
			PojoModelPath currentPath = path;
			do {
				if ( currentPath instanceof PojoModelPathValueNode ) {
					components.addFirst( ( (PojoModelPathValueNode) currentPath ).getExtractorPath() );
				}
				else {
					components.addFirst( ( (PojoModelPathPropertyNode) currentPath ).getPropertyName() );
				}
				currentPath = currentPath.getParent();
			}
			while ( currentPath != null );

			assertThat( components ).containsExactly( pathComponents );
		};
	}

}