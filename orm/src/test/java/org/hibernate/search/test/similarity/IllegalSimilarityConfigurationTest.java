/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.similarity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Similarity;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies that SearchFactory properly checks for illegal combinations
 * of Similarity: different entities sharing the same index must use the same
 * Similarity implementation.
 * Also when opening a {@code MultiReader} on multiple indexes, all of these should
 * use the same Similarity implementation.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public class IllegalSimilarityConfigurationTest {
	FullTextSessionBuilder builder = null;

	@After
	public void tearDown() {
		if ( builder != null ) {
			builder.close();
		}
	}

	@Test
	public void testValidConfiguration() {
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Can.class )
					.addAnnotatedClass( Trash.class )
					.build();
		}
		catch (SearchException e) {
			fail( "A valid configuration could not be started." );
		}
	}

	@Test
	public void testInconsistentAnnotationAndExplicitIndexSimilarityThrowsException() {
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Trash.class )
					.setProperty( "hibernate.search.garbageIndex.similarity", DummySimilarity2.class.getName() )
					.build();
			fail(
					"Invalid Similarity declared, should have thrown an exception: entities similarity"
							+ " defined as annotation and config value"
			);
		}
		catch (SearchException e) {
			assertTrue( "Unexpected message: " + e.getMessage() , e.getMessage().startsWith( "HSEARCH000188" ) );
		}
	}

	@Test
	public void testInconsistentAnnotationAndIndexDefaultSimilarityThrowsException() {
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Trash.class )
					.setProperty( "hibernate.search.default.similarity", DummySimilarity2.class.getName() )
					.build();
			fail(
					"Invalid Similarity declared, should have thrown an exception: entities similarity"
							+ " defined as annotation and config value"
			);
		}
		catch (SearchException e) {
			assertTrue( "Unexpected message: " + e.getMessage() , e.getMessage().startsWith( "HSEARCH000188" ) );
		}
	}

	@Test
	public void testInconsistentSimilarityInClassSharingAnIndex() {
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Trash.class )
					.addAnnotatedClass( Sink.class )
					.build();
			fail(
					"Invalid Similarity declared, should have thrown an exception: two entities"
							+ " sharing the same index are using a different similarity"
			);
		}
		catch (SearchException e) {
			assertTrue( "Unexpected message: " + e.getMessage() , e.getMessage().startsWith( "HSEARCH000189" ) );
		}
	}

	@Test
	public void testImplicitSimilarityInheritanceIsValid() {
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Trash.class )
					.addAnnotatedClass( ProperTrashExtension.class )
					.build();
		}
		catch (SearchException e) {
			fail( "Valid configuration could not be built" );
		}
	}

	@Test
	public void testImplicitInconsistentSimilarityInClassHierarchy() {

		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Can.class )
					.addAnnotatedClass( SmallerCan.class )
					.build();
			fail(
					"Invalid Similarity declared, should have thrown an exception: child entity"
							+ " is overriding parent's Similarity"
			);
		}
		catch (SearchException e) {
			assertTrue( "Unexpected message: " + e.getMessage() , e.getMessage().startsWith( "HSEARCH000189" ) );
		}
	}

	@Test
	public void testExplicitInconsistentSimilarityInClassHierarchy() {
		try {
			builder = new FullTextSessionBuilder()
					.addAnnotatedClass( Parent.class )
					.addAnnotatedClass( Child.class )
					.build();
			fail(
					"Invalid Similarity declared, should have thrown an exception: same similarity"
							+ " must be used across class hierarchy"
			);
		}
		catch (SearchException e) {
			assertTrue( "Unexpected message: " + e.getMessage() , e.getMessage().startsWith( "HSEARCH000187" ) );
		}
	}

	@Entity
	@Similarity(impl = DummySimilarity2.class)
	public class Parent {
		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity
	@Indexed
	@Similarity(impl = DummySimilarity.class)
	public class Child extends Parent {
	}
}
