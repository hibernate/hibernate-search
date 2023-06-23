/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.path.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.util.common.SearchException;

import org.junit.Rule;
import org.junit.Test;

/**
 * @author zkurey
 */
public class TestInvalidPaths {

	@Rule
	public final FullTextSessionBuilder cfg = new FullTextSessionBuilder();

	@Test
	public void testInvalidDeepSimplePath() {
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
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( DeepPathWithLeadingPrefixCase.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for DeepPathWithLeadingPrefixCase having invalid path: b.c.dne" );
		}
		catch (SearchException se) {
			assertThat( se )
					.hasMessageContainingAll(
							"type '" + DeepPathWithLeadingPrefixCase.class.getName() + "'",
							"Non-matching includePaths filters: [b.c.dne]",
							"Encountered field paths: [b, b.c, b.c.indexed, prefixedc.indexed]"
					);
		}
	}

	@Test
	public void testInvalidPrefix() {
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
	public void testEmbeddedPathValidation() {
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
			assertThat( se )
					.hasMessageContainingAll(
							"type '" + InvalidEmbeddedPathCase.class.getName() + "'",
							"Non-matching includePaths filters: [emb.e4]",
							"Encountered field paths: [emb, emb.e1, emb.e3, emb.e3.c, emb.e3.c.indexed, emb.e3.c.notIndexed]"
					);
		}
	}

	@Test
	public void testNonLeafEmbeddedPath() {
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
			assertThat( se )
					.hasMessageContainingAll(
							"type '" + InvalidEmbeddedNonLeafCase.class.getName() + "'",
							"Non-matching includePaths filters: [emb.e4]",
							"Encountered field paths: [emb, emb.e1, emb.e3, emb.e3.c, emb.e3.c.indexed, emb.e3.c.notIndexed]"
					);
		}
	}

	@Test
	public void testNonIndexedPath() {
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
							+ se.getMessage(),
					se.getMessage().contains( "c.indexed" ) );
		}
	}

	@Test
	public void testRenamedFieldInPath() {
		cfg.addAnnotatedClass( FieldRenamedContainerEntity.class );
		cfg.addAnnotatedClass( FieldRenamedEmbeddedEntity.class );
		try {
			cfg.build();
			fail( "Exception should have been thrown for FieldRenamedContainerEntity having invalid path (attribute instead of field name): embedded.field" );
		}
		catch (SearchException se) {
			assertTrue(
					"Expected search exception to contain information about invalid leaf path embedded.field, instead got error: "
							+ se.getMessage(),
					se.getMessage().contains( "embedded.field" ) );
		}
	}

	/**
	 * Ensures that path still marked as encountered if depth is the cause of the path being
	 * traversed, and they are the same depth
	 */
	@Test
	public void testDepthMatchesPathMarkedAsEncountered() {
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
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( ReferencesIndexedEmbeddedA.class );
		cfg.addAnnotatedClass( DepthExceedsPathTestCase.class );
		cfg.build();
	}

}
