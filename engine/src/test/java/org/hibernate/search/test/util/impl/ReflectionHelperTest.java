/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import java.lang.annotation.Annotation;
import java.util.List;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
	public class D {
	}

	public class E {
	}
}
