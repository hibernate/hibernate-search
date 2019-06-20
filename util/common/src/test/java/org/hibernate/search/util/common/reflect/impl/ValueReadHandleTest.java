/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ValueReadHandleTest {

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> data() {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		return Arrays.asList( new Object[][] {
				{ ValueReadHandleFactory.usingMethodHandle( lookup ) },
				{ ValueReadHandleFactory.usingJavaLangReflect() }
		} );
	}

	private final ValueReadHandleFactory factory;

	public ValueReadHandleTest(ValueReadHandleFactory factory) {
		this.factory = factory;
	}

	@Test
	public void privateField() throws Exception {
		testFieldValueReadHandle( "privateField" );
	}

	@Test
	public void privateFinalField() throws Exception {
		testFieldValueReadHandle( "privateFinalField" );
	}

	@Test
	public void packagePrivateField() throws Exception {
		testFieldValueReadHandle( "packagePrivateField" );
	}

	@Test
	public void packagePrivateFinalField() throws Exception {
		testFieldValueReadHandle( "packagePrivateFinalField" );
	}

	@Test
	public void protectedField() throws Exception {
		testFieldValueReadHandle( "protectedField" );
	}

	@Test
	public void protectedFinalField() throws Exception {
		testFieldValueReadHandle( "protectedFinalField" );
	}

	@Test
	public void publicField() throws Exception {
		testFieldValueReadHandle( "publicField" );
	}

	@Test
	public void publicFinalField() throws Exception {
		testFieldValueReadHandle( "publicFinalField" );
	}

	@Test
	public void privateMethod() throws Exception {
		testMethodValueReadHandle( "privateMethod" );
	}

	@Test
	public void packagePrivateMethod() throws Exception {
		testMethodValueReadHandle( "packagePrivateMethod" );
	}

	@Test
	public void protectedMethod() throws Exception {
		testMethodValueReadHandle( "protectedMethod" );
	}

	@Test
	public void publicMethod() throws Exception {
		testMethodValueReadHandle( "publicMethod" );
	}

	private void testFieldValueReadHandle(String fieldName) throws IllegalAccessException, NoSuchFieldException {
		String expectedValue = fieldName + "Value";
		Field field = EntityType.class.getDeclaredField( fieldName );
		setAccessible( field );
		Field otherField = EntityType.class.getDeclaredField( "otherField" );
		setAccessible( otherField );

		ValueReadHandle<?> valueReadHandle = factory.createForField( field );

		assertThat( valueReadHandle.get( new EntityType() ) ).isEqualTo( expectedValue );

		assertThat( valueReadHandle.toString() )
				.contains( valueReadHandle.getClass().getSimpleName() )
				.contains( field.toString() );

		ValueReadHandle<?> equalValueReadHandle = factory.createForField( field );
		ValueReadHandle<?> differentFieldValueReadHandle = factory.createForField( otherField );
		assertThat( valueReadHandle ).isEqualTo( equalValueReadHandle );
		assertThat( valueReadHandle.hashCode() ).isEqualTo( equalValueReadHandle.hashCode() );
		assertThat( valueReadHandle ).isNotEqualTo( differentFieldValueReadHandle );
	}

	private void testMethodValueReadHandle(String methodName) throws IllegalAccessException, NoSuchMethodException {
		String expectedValue = methodName + "Value";
		Method method = EntityType.class.getDeclaredMethod( methodName );
		setAccessible( method );
		Method otherMethod = EntityType.class.getDeclaredMethod( "otherMethod" );
		setAccessible( otherMethod );

		ValueReadHandle<?> valueReadHandle = factory.createForMethod( method );
		assertThat( valueReadHandle.get( new EntityType() ) ).isEqualTo( expectedValue );

		assertThat( valueReadHandle.toString() )
				.contains( valueReadHandle.getClass().getSimpleName() )
				.contains( method.toString() );

		ValueReadHandle<?> equalValueReadHandle = factory.createForMethod( method );
		ValueReadHandle<?> differentMethodValueReadHandle = factory.createForMethod( otherMethod );
		assertThat( valueReadHandle ).isEqualTo( equalValueReadHandle );
		assertThat( valueReadHandle.hashCode() ).isEqualTo( equalValueReadHandle.hashCode() );
		assertThat( valueReadHandle ).isNotEqualTo( differentMethodValueReadHandle );
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