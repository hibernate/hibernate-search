/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.embedded.path.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hibernate.search.SearchException;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Test;

/**
 * @author zkurey
 */
public class TestInvalidPaths {

	@Test
	public void testInvalidDeepSimplePath() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( DeepPathSimpleTypeCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for DeepPathSimpleTypeCase having invalid path: b.c.dne" );
		}
		catch (SearchException se) {
			assertTrue( "Expected search exception to contain information about invalid path b.c.dne",
					se.getMessage().contains( "b.c.dne" ) );
		}
	}

	@Test
	public void testInvalidDeepSimplePathWithLeadingPrefix() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( DeepPathWithLeadingPrefixCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for DeepPathWithLeadingPrefixCase having invalid path: b.c.dne" );
		}
		catch (SearchException se) {
			assertTrue( "Should contain information about invalid path b.c.dne (message: <" + se.getMessage() + ">)" ,
					se.getMessage().matches( ".*\\sb.c.dne.*" ) );
			assertFalse( "Should NOT contain information about invalid path prefix: notJustA (message: <" + se.getMessage() + ">)",
					se.getMessage().contains( "notJustA" ) );
		}
	}

	@Test
	public void testInvalidPrefix() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidPrefixCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for InvalidPrefixCase having invalid path: b.c.dne" );
		}
		catch (SearchException se) {
			assertTrue( "Expected search exception to contain information about invalid path a.b.c.indexed",
					se.getMessage().contains( "a.b.c.indexed" ) );
		}
	}

	@Test
	public void testShallowInvalidPath() throws Exception {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidShallowPathCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for ShallowPathCase having invalid path: dne" );
		}
		catch (SearchException se) {
			assertTrue( "Expected search exception to contain information about invalid path dne",
					se.getMessage().contains( "dne" ) );
		}
	}

	@Test
	public void testNonLeafPathInvalid() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidNonLeafUseCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for D having invalid path: b.c" );
		}
		catch (SearchException se) {
			assertTrue( "Expected search exception to contain information about invalid path b.c",
					se.getMessage().contains( "b.c" ) );
		}
	}

	@Test
	public void testEmbeddedPathValidation() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidEmbeddedWithoutPathsCase.class );
		cfg.addAnnotatedClass( InvalidEmbeddedPathCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for InvalidEmbeddedPathsCase having invalid path: emb.e4" );
		}
		catch (SearchException se) {
			assertTrue(
					"Expected search exception to contain information about invalid path emb.e4, instead got error: "
							+ se.getMessage(), se.getMessage().contains( "emb.e4" ) );
			assertFalse(
					"Expected search exception to NOT contain information about invalid path emb.e1, instead got error: "
							+ se.getMessage(), se.getMessage().contains( "emb.e1" ) );
		}
	}

	@Test
	public void testNonLeafEmbeddedPath() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidEmbeddedWithoutPathsCase.class );
		cfg.addAnnotatedClass( InvalidEmbeddedNonLeafCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for InvalidEmbeddedNonLeafCase having invalid path: emb.e3" );
		}
		catch (SearchException se) {
			assertTrue(
					"Expected search exception to contain information about invalid leaf path emb.e3, instead got error: "
							+ se.getMessage(), se.getMessage().contains( "emb.e3" ) );
			assertFalse(
					"Expected search exception to NOT contain information about invalid path emb.e1, instead got error: "
							+ se.getMessage(), se.getMessage().contains( "emb.e1" ) );
		}
	}

	@Test
	public void testNonIndexedPath() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( ReferencesC.class );
		cfg.addAnnotatedClass( PathNotIndexedCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for PathNotIndexedCase having invalid path: c.indexed" );
		}
		catch (SearchException se) {
			assertTrue(
					"Expected search exception to contain information about invalid leaf path c.indexed, instead got error: "
							+ se.getMessage(), se.getMessage().contains( "c.indexed" ) );
		}
	}

	@Test
	public void testRenamedFieldInPath() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( FieldRenamedContainerEntity.class );
		cfg.addAnnotatedClass( FieldRenamedEmbeddedEntity.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for FieldRenamedContainerEntity having invalid path (attribute instead of field name): embedded.field" );
		}
		catch (SearchException se) {
			assertTrue(
					"Expected search exception to contain information about invalid leaf path embedded.field, instead got error: "
							+ se.getMessage(), se.getMessage().contains( "embedded.field" ) );
		}
	}

	/**
	 * Ensures that path still marked as encountered if depth is the cause of the path being
	 * traversed, and they are the same depth
	 */
	@Test
	public void testDepthMatchesPathMarkedAsEncountered() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( ReferencesIndexedEmbeddedA.class );
		cfg.addAnnotatedClass( DepthMatchesPathDepthCase.class );
		cfg.build();
	}

	/**
	 * Ensures that path still marked as encountered if depth is the cause of the path being
	 * traversed, and depth exceeds path depth
	 */
	@Test
	public void testDepthExceedsPathMarkedAsEncountered() {
		FullTextSessionBuilder cfg = new FullTextSessionBuilder();
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( ReferencesIndexedEmbeddedA.class );
		cfg.addAnnotatedClass( DepthExceedsPathTestCase.class );
		cfg.build();
	}

}
