/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
