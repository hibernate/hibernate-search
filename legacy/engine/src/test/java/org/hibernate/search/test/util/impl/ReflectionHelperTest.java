/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.Test;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.impl.ReflectionHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class ReflectionHelperTest {

	@Test
	public void testIsSearchAnnotation() throws Exception {
		AnnotationDescriptor descriptor = new AnnotationDescriptor( IndexedEmbedded.class );
		Annotation annotation = AnnotationFactory.create( descriptor );
		assertTrue( ReflectionHelper.isSearchAnnotation( annotation ) );

		descriptor = new AnnotationDescriptor( Override.class );
		annotation = AnnotationFactory.create( descriptor );
		assertFalse( ReflectionHelper.isSearchAnnotation( annotation ) );
	}

	@Test
	public void testIsSearchEnabled() throws Exception {
		ReflectionManager reflectionManager = new JavaReflectionManager();

		assertTrue(
				"Should be a search enabled class",
				ReflectionHelper.containsSearchAnnotations( reflectionManager.toXClass( A.class ) )
		);
		assertTrue(
				"Should be a search enabled class",
				ReflectionHelper.containsSearchAnnotations( reflectionManager.toXClass( B.class ) )
		);
		assertTrue(
				"Should be a search enabled class",
				ReflectionHelper.containsSearchAnnotations( reflectionManager.toXClass( C.class ) )
		);
		assertTrue(
				"Should be a search enabled class",
				ReflectionHelper.containsSearchAnnotations( reflectionManager.toXClass( D.class ) )
		);

		assertFalse(
				"Should not be a search enabled class",
				ReflectionHelper.containsSearchAnnotations( reflectionManager.toXClass( E.class ) )
		);
	}

	@Test
	public void testCreateInstanceWithClassHavingPrivateNoArgConstructorThrowsException() {
		try {
			ReflectionHelper.createInstance( F.class, false );
			fail( "It should not be possible to create a class with no public no-args constructor." );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000242" ) );
		}
	}

	@Test
	public void testCreateInstanceWithClassHavingNoNoArgConstructorThrowsException() {
		try {
			ReflectionHelper.createInstance( G.class, false );
			fail( "It should not be possible to create a class with no public no-args constructor." );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000242" ) );
		}
	}

	@Test
	public void testUsingMultipleFactoryAnnotationsThrowsException() {
		try {
			ReflectionHelper.createInstance( H.class, true );
			fail( "More than once @Factory annotation should throw an exception." );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000241" ) );
		}
	}

	@Test
	public void testFactoryMethodWithNoReturnTypeThrowsException() {
		try {
			ReflectionHelper.createInstance( I.class, true );
			fail( "Factory methods w/o return type should throw an exception." );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000244" ) );
		}
	}

	@Test
	public void testSuccessfulExecutionOfFactoryMethod() {
		assertTrue( "The factory method return type was expected.", ReflectionHelper.createInstance( K.class, true ) instanceof D );
	}

	@Test
	public void testSuccessfulInstantiationOfClass() {
		assertTrue( "The factory method should not be executed", ReflectionHelper.createInstance( K.class, false ) instanceof K );
	}

	public class A {
		@Field
		private String name;
	}

	public class B {
		@IndexedEmbedded
		private List<A> aList;
	}

	public class C {
		@FieldBridge
		public String getFoo() {
			return null;
		}
	}

	@Analyzer
	public static class D {
	}

	public class E {
	}

	public class F {
		private F() {
		}
	}

	public class G {
		private G(String foo) {
		}
	}

	public static class H {
		@Factory
		public Object foo() {
			return new Object();
		}

		@Factory
		public Object bar() {
			return new Object();
		}
	}

	public static class I {
		@Factory
		public void foo() {
		}
	}

	public static class J {
		@Factory
		public Object foo() {
			throw new IllegalArgumentException( );
		}
	}

	public static class K {
		@Factory
		public D create() {
			return new D();
		}
	}
}
