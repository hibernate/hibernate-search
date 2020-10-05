/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.common.testsupport.GenericTypeContextAssert.assertThatTypeContext;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.util.common.testsupport.GenericTypeContextAssert;
import org.hibernate.search.util.impl.test.reflect.TypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture;
import org.hibernate.search.util.impl.test.reflect.WildcardTypeCapture.Of;

import org.junit.Test;

@SuppressWarnings("unused")
public class GenericTypeContextTest {

	@Test
	public void simple() {
		new GenericTypeContextAssert.AssertWithType<Iterable<CustomType>>() {
		}
				.resolveTypeArgumentTo( CustomType.class, Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
		new GenericTypeContextAssert.AssertWithType<Collection<CustomType>>() {
		}
				.resolveTypeArgumentTo( CustomType.class, Iterable.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Collection.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
		new GenericTypeContextAssert.AssertWithType<List<CustomType>>() {
		}
				.resolveTypeArgumentTo( CustomType.class, Iterable.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Collection.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
		new GenericTypeContextAssert.AssertWithType<ArrayList<CustomType>>() {
		}
				.resolveTypeArgumentTo( CustomType.class, Iterable.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Collection.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, List.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, ArrayList.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
		new GenericTypeContextAssert.AssertWithType<Map<String, CustomType>>() {
		}
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Map.class, 1 );
		new GenericTypeContextAssert.AssertWithType<HashMap<String, CustomType>>() {
		}
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, Map.class, 1 );
	}

	@Test
	public void nullType() {
		assertThatThrownBy( () -> new GenericTypeContext( null ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	public void nullType_nonNullContext() {
		GenericTypeContext declaringContext = new GenericTypeContext( Object.class );

		assertThatThrownBy( () -> new GenericTypeContext( declaringContext, null ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	public void genericArgument() {
		new GenericTypeContextAssert.AssertWithType<Iterable<CustomGenericType<String, Integer>>>() {
		}
				.resolveTypeArgumentTo( new TypeCapture<CustomGenericType<String, Integer>>() {
				}, Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void rawType() {
		new GenericTypeContextAssert.AssertWithType<Collection>() {
		}
				.resolveTypeArgumentTo(
						Collection.class.getTypeParameters()[0],
						Iterable.class, 0
				)
				.resolveTypeArgumentTo(
						Collection.class.getTypeParameters()[0],
						Collection.class, 0
				)
				.resolveTypeArgumentToEmpty( List.class, 0 );
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void boundedRawType() {
		new GenericTypeContextAssert.AssertWithType<CustomBoundedGenericInterface>() {
		}
				.resolveTypeArgumentTo(
						CustomBoundedGenericInterface.class.getTypeParameters()[0],
						CustomBoundedGenericInterface.class, 0
				);
	}

	@Test
	public void swappedArguments() {
		new GenericTypeContextAssert.AssertWithType<CustomGenericType<String, Integer>>() {
		}
				.resolveTypeArgumentTo( String.class, CustomGenericType.class, 0 )
				.resolveTypeArgumentTo( Integer.class, CustomGenericType.class, 1 )
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 );
	}

	@Test
	public void fixedArguments() {
		new GenericTypeContextAssert.AssertWithType<CustomArgumentSettingType>() {
		}
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( CustomType.class, CustomGenericInterface.class, 1 );
	}

	@Test
	public <T> void unboundedTypeVariable() {
		// Type variable as the tested type
		new GenericTypeContextAssert.AssertWithType<T>() {
		}
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Type variable as an argument to the tested type
		new GenericTypeContextAssert.AssertWithType<Map<T, String>>() {
		}
				.resolveTypeArgumentTo( Object.class, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public <T extends CustomGenericInterface<Integer, String>> void singleUpperBoundTypeVariable() {
		// Type variable as the tested type
		new GenericTypeContextAssert.AssertWithType<T>() {
		}
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Type variable as an argument to the tested type
		new GenericTypeContextAssert.AssertWithType<Map<T, String>>() {
		}
				.resolveTypeArgumentTo( new TypeCapture<CustomGenericInterface<Integer, String>>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public <T extends CustomGenericInterface<Integer, String> & Collection<Double>> void multipleUpperBoundsTypeVariable() {
		// Type variable as the tested type
		new GenericTypeContextAssert.AssertWithType<T>() {
		}
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentTo( Double.class, Iterable.class, 0 )
				.resolveTypeArgumentTo( Double.class, Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Type variable as an argument to the tested type
		new GenericTypeContextAssert.AssertWithType<Map<T, String>>() {
		}
				.resolveTypeArgumentTo( new TypeCapture<CustomGenericInterface<Integer, String>>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public void unboundedWildcard() {
		// Wildcard as the tested type
		new GenericTypeContextAssert.AssertWithWildcardType<Of<?>>() {
		}
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Wildcard as an argument to the tested type
		new GenericTypeContextAssert.AssertWithType<CustomGenericType<?, String>>() {
		}
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( new WildcardTypeCapture<Of<?>>() {
				}, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
	}

	@Test
	public void singleUpperBoundWildcard() {
		// Wildcard as the tested type
		new GenericTypeContextAssert.AssertWithWildcardType<Of<? extends CustomGenericInterface<Integer, String>>>() {
		}
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Wildcard as an argument to the tested type
		new GenericTypeContextAssert.AssertWithType<Map<? extends CustomGenericInterface<Integer, String>, String>>() {
		}
				.resolveTypeArgumentTo( new WildcardTypeCapture<Of<? extends CustomGenericInterface<Integer, String>>>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public <T> void unboundedTypeVariableUpperBoundWildcard() {
		// Wildcard as the tested type
		new GenericTypeContextAssert.AssertWithWildcardType<Of<? extends T>>() {
		}
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Wildcard as an argument to the tested type
		new GenericTypeContextAssert.AssertWithType<CustomGenericInterface<? extends T, String>>() {
		}
				.resolveTypeArgumentTo( new WildcardTypeCapture<Of<? extends T>>() {
				}, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );
	}

	@Test
	public <T extends CustomGenericInterface<Integer, String>> void boundedTypeVariableUpperBoundWildcard() {
		// Wildcard as the tested type
		new GenericTypeContextAssert.AssertWithWildcardType<Of<? extends T>>() {
		}
				.resolveTypeArgumentTo( Integer.class, CustomGenericInterface.class, 0 )
				.resolveTypeArgumentTo( String.class, CustomGenericInterface.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 0 )
				.resolveTypeArgumentToEmpty( Map.class, 1 );

		// Wildcard as an argument to the tested type
		new GenericTypeContextAssert.AssertWithType<Map<? extends T, String>>() {
		}
				.resolveTypeArgumentTo( new WildcardTypeCapture<Of<? extends T>>() {
				}, Map.class, 0 )
				.resolveTypeArgumentTo( String.class, Map.class, 1 )
				.resolveTypeArgumentToEmpty( Iterable.class, 0 )
				.resolveTypeArgumentToEmpty( Collection.class, 0 )
				.resolveTypeArgumentToEmpty( List.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 0 )
				.resolveTypeArgumentToEmpty( CustomGenericInterface.class, 1 );
	}

	@Test
	public void badIndex() {
		new GenericTypeContextAssert.AssertWithType<CustomGenericType<String, Integer>>() {
		}
				.typeParameterIndexTooHigh( CustomGenericInterface.class, 2 )
				.typeParameterIndexTooHigh( CustomGenericInterface.class, 42 )
				.typeParameterIndexTooHigh( CustomGenericInterface.class, Integer.MAX_VALUE )
				.typeParameterIndexTooHigh( Collection.class, 1 )
				.typeParameterIndexTooLow( Collection.class, -1 )
				.typeParameterIndexTooLow( Collection.class, Integer.MIN_VALUE )
				.noTypeParameter( String.class, 0 )
				.noTypeParameter( CustomArgumentSettingType.class, 0 );
	}

	@Test
	public void declaringContext() throws NoSuchFieldException {
		class GenericDeclaringClass<T> {
			public List<T> property;
		}
		class TypeSettingClass extends GenericDeclaringClass<String> {
		}

		Type propertyType = GenericDeclaringClass.class.getField( "property" ).getGenericType();
		GenericTypeContext typeSettingClassContext = new GenericTypeContext( TypeSettingClass.class );

		GenericTypeContext propertyContext = new GenericTypeContext( typeSettingClassContext, propertyType );
		assertThatTypeContext( propertyContext )
				.resolveTypeArgumentTo( String.class, List.class, 0 );
	}

	@Test
	public void declaringContext_array() throws NoSuchFieldException {
		class GenericDeclaringClass<T> {
			public T[] arrayProperty;
			public T nonArrayProperty;
		}
		class TypeSettingClass extends GenericDeclaringClass<String> {
		}

		Type arrayPropertyType = GenericDeclaringClass.class.getField( "arrayProperty" ).getGenericType();
		Type nonArrayPropertyType = GenericDeclaringClass.class.getField( "nonArrayProperty" ).getGenericType();
		GenericTypeContext typeSettingClassContext = new GenericTypeContext( TypeSettingClass.class );

		GenericTypeContext arrayPropertyContext = new GenericTypeContext( typeSettingClassContext, arrayPropertyType );
		assertThatTypeContext( arrayPropertyContext )
				.resolveArrayElementTypeTo( String.class );

		GenericTypeContext nonArrayPropertyContext = new GenericTypeContext( typeSettingClassContext, nonArrayPropertyType );
		assertThatTypeContext( nonArrayPropertyContext )
				.resolveArrayElementTypeToEmpty();
	}

	@Test
	public void declaringContext_multiNesting() throws NoSuchFieldException {
		class GenericDeclaringLevel2Class<T> {
			public T property;
		}
		class GenericDeclaringLevel1Class<T> {
			public GenericDeclaringLevel2Class<List<T>> property;
		}
		class TypeSettingClass extends GenericDeclaringLevel1Class<String> {
		}

		Type level1PropertyType = GenericDeclaringLevel1Class.class.getField( "property" ).getGenericType();
		Type level2PropertyType = GenericDeclaringLevel2Class.class.getField( "property" ).getGenericType();
		GenericTypeContext typeSettingClassContext = new GenericTypeContext( TypeSettingClass.class );

		GenericTypeContext property1Context = new GenericTypeContext( typeSettingClassContext, level1PropertyType );

		GenericTypeContext property2Context = new GenericTypeContext( property1Context, level2PropertyType );
		assertThatTypeContext( property2Context )
				.resolveTypeArgumentTo( String.class, List.class, 0 )
				// Ensure we don't cascade to the declaring context when resolving type arguments
				.resolveTypeArgumentToEmpty( GenericDeclaringLevel1Class.class, 0 );
	}

	@Test
	public void declaringContext_genericMethod() throws NoSuchMethodException {
		abstract class GenericDeclaringClass<T> {
			public abstract <U extends T> List<U> property();
		}
		abstract class TypeSettingClass extends GenericDeclaringClass<String> {
		}

		Type propertyType = GenericDeclaringClass.class.getMethod( "property" ).getGenericReturnType();
		GenericTypeContext declaringContext = new GenericTypeContext( TypeSettingClass.class );

		GenericTypeContext propertyContext = new GenericTypeContext( declaringContext, propertyType );
		assertThatTypeContext( propertyContext )
				.resolveTypeArgumentTo( String.class, List.class, 0 );
	}

	@Test
	public void declaringContext_relatedParameters() throws NoSuchMethodException {
		abstract class GenericDeclaringClass<T, U extends List<T>> {
			public abstract U property();
		}
		abstract class GenericSubClass<V extends List<String>> extends GenericDeclaringClass<String, V> {
		}
		abstract class TypeSettingClass extends GenericSubClass<ArrayList<String>> {
		}

		Type propertyType = GenericDeclaringClass.class.getMethod( "property" ).getGenericReturnType();
		GenericTypeContext genericSubClassContext = new GenericTypeContext( GenericSubClass.class );
		GenericTypeContext typeSettingContext = new GenericTypeContext( TypeSettingClass.class );

		GenericTypeContext propertyInGenericSubClassContext =
				new GenericTypeContext( genericSubClassContext, propertyType );
		assertThatTypeContext( propertyInGenericSubClassContext )
				.resolveTypeTo( GenericSubClass.class.getTypeParameters()[0] )
				.resolveTypeArgumentTo( String.class, List.class, 0 )
				.resolveTypeArgumentToEmpty( ArrayList.class, 0 );

		GenericTypeContext propertyInTypeSettingContext = new GenericTypeContext( typeSettingContext, propertyType );
		assertThatTypeContext( propertyInTypeSettingContext )
				.resolveTypeTo( new TypeCapture<ArrayList<String>>() { } )
				.resolveTypeArgumentTo( String.class, List.class, 0 )
				.resolveTypeArgumentTo( String.class, ArrayList.class, 0 );
	}

}
