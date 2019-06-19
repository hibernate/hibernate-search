/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PropertyHandleTest {

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> data() {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		return Arrays.asList( new Object[][] {
				{ PropertyHandleFactory.usingMethodHandle( lookup ) },
				{ PropertyHandleFactory.usingJavaLangReflect() }
		} );
	}

	private final PropertyHandleFactory factory;

	public PropertyHandleTest(PropertyHandleFactory factory) {
		this.factory = factory;
	}

	@Test
	public void privateField() throws Exception {
		testFieldPropertyHandle( "privateField" );
	}

	@Test
	public void privateFinalField() throws Exception {
		testFieldPropertyHandle( "privateFinalField" );
	}

	@Test
	public void packagePrivateField() throws Exception {
		testFieldPropertyHandle( "packagePrivateField" );
	}

	@Test
	public void packagePrivateFinalField() throws Exception {
		testFieldPropertyHandle( "packagePrivateFinalField" );
	}

	@Test
	public void protectedField() throws Exception {
		testFieldPropertyHandle( "protectedField" );
	}

	@Test
	public void protectedFinalField() throws Exception {
		testFieldPropertyHandle( "protectedFinalField" );
	}

	@Test
	public void publicField() throws Exception {
		testFieldPropertyHandle( "publicField" );
	}

	@Test
	public void publicFinalField() throws Exception {
		testFieldPropertyHandle( "publicFinalField" );
	}

	@Test
	public void privateMethod() throws Exception {
		testMethodPropertyHandle( "privateMethod" );
	}

	@Test
	public void packagePrivateMethod() throws Exception {
		testMethodPropertyHandle( "packagePrivateMethod" );
	}

	@Test
	public void protectedMethod() throws Exception {
		testMethodPropertyHandle( "protectedMethod" );
	}

	@Test
	public void publicMethod() throws Exception {
		testMethodPropertyHandle( "publicMethod" );
	}

	private void testFieldPropertyHandle(String fieldName) throws IllegalAccessException, NoSuchFieldException {
		String expectedValue = fieldName + "Value";
		Field field = EntityType.class.getDeclaredField( fieldName );
		setAccessible( field );
		Field otherField = EntityType.class.getDeclaredField( "otherField" );
		setAccessible( otherField );

		PropertyHandle<?> propertyHandle = factory.createForField( field );

		assertThat( propertyHandle.get( new EntityType() ) ).isEqualTo( expectedValue );

		assertThat( propertyHandle.toString() )
				.contains( propertyHandle.getClass().getSimpleName() )
				.contains( field.toString() );

		PropertyHandle<?> equalPropertyHandle = factory.createForField( field );
		PropertyHandle<?> differentFieldPropertyHandle = factory.createForField( otherField );
		assertThat( propertyHandle ).isEqualTo( equalPropertyHandle );
		assertThat( propertyHandle.hashCode() ).isEqualTo( equalPropertyHandle.hashCode() );
		assertThat( propertyHandle ).isNotEqualTo( differentFieldPropertyHandle );
	}

	private void testMethodPropertyHandle(String methodName) throws IllegalAccessException, NoSuchMethodException {
		String expectedValue = methodName + "Value";
		Method method = EntityType.class.getDeclaredMethod( methodName );
		setAccessible( method );
		Method otherMethod = EntityType.class.getDeclaredMethod( "otherMethod" );
		setAccessible( otherMethod );

		PropertyHandle<?> propertyHandle = factory.createForMethod( method );
		assertThat( propertyHandle.get( new EntityType() ) ).isEqualTo( expectedValue );

		assertThat( propertyHandle.toString() )
				.contains( propertyHandle.getClass().getSimpleName() )
				.contains( method.toString() );

		PropertyHandle<?> equalPropertyHandle = factory.createForMethod( method );
		PropertyHandle<?> differentMethodPropertyHandle = factory.createForMethod( otherMethod );
		assertThat( propertyHandle ).isEqualTo( equalPropertyHandle );
		assertThat( propertyHandle.hashCode() ).isEqualTo( equalPropertyHandle.hashCode() );
		assertThat( propertyHandle ).isNotEqualTo( differentMethodPropertyHandle );
	}

	private static void setAccessible(Member member) {
		if ( !Modifier.isPublic( member.getModifiers() ) ) {
			( (AccessibleObject) member ).setAccessible( true );
		}
	}

	private static class EntityType {
		private String privateField = "privateFieldValue";
		private final String privateFinalField = "privateFinalFieldValue";
		String packagePrivateField = "packagePrivateFieldValue";
		final String packagePrivateFinalField = "packagePrivateFinalFieldValue";
		protected String protectedField = "protectedFieldValue";
		protected final String protectedFinalField = "protectedFinalFieldValue";
		public String publicField = "publicFieldValue";
		public final String publicFinalField = "publicFinalFieldValue";
		private String otherField;

		private String privateMethod() {
			return "privateMethodValue";
		}
		String packagePrivateMethod() {
			return "packagePrivateMethodValue";
		}
		protected String protectedMethod() {
			return "protectedMethodValue";
		}
		public String publicMethod() {
			return "publicMethodValue";
		}
		public String otherMethod() {
			return "otherMethod";
		}
	}
}