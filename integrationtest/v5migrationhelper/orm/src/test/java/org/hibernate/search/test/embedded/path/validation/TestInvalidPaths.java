/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author zkurey
 */
class TestInvalidPaths {

	@RegisterExtension
	public final FullTextSessionBuilder cfg = new FullTextSessionBuilder();

	@Test
	void testInvalidDeepSimplePath() {
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( DeepPathSimpleTypeCase.class );
		assertThatThrownBy( () -> cfg.build() )
				.as( "Exception should have been thrown for DeepPathSimpleTypeCase having invalid path: b.c.dne" )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"b.c.dne"
				);
	}

	@Test
	void testInvalidDeepSimplePathWithLeadingPrefix() {
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( DeepPathWithLeadingPrefixCase.class );
		assertThatThrownBy( () -> cfg.build() )
				.as( "Exception should have been thrown for DeepPathWithLeadingPrefixCase having invalid path: b.c.dne" )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"type '" + DeepPathWithLeadingPrefixCase.class.getName() + "'",
						"Non-matching includePaths filters: [b.c.dne]",
						"Encountered field paths: [b, b.c, b.c.indexed, prefixedc.indexed]"
				);
	}

	@Test
	void testInvalidPrefix() {
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidPrefixCase.class );
		assertThatThrownBy( () -> cfg.build() )
				.as( "Expected search exception to contain information about invalid path a.b.c.indexed" )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"a.b.c.indexed"
				);
	}

	@Test
	void testShallowInvalidPath() {
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidShallowPathCase.class );
		assertThatThrownBy( () -> cfg.build() )
				.as( "Expected search exception to contain information about invalid path dne" )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"dne"
				);
	}

	@Test
	void testEmbeddedPathValidation() {
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidEmbeddedWithoutPathsCase.class );
		cfg.addAnnotatedClass( InvalidEmbeddedPathCase.class );
		assertThatThrownBy( () -> cfg.build() )
				.as( "Exception should have been thrown for InvalidEmbeddedPathsCase having invalid path: emb.e4" )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"type '" + InvalidEmbeddedPathCase.class.getName() + "'",
						"Non-matching includePaths filters: [emb.e4]",
						"Encountered field paths: [emb, emb.e1, emb.e3, emb.e3.c, emb.e3.c.indexed, emb.e3.c.notIndexed]"
				);
	}

	@Test
	void testNonLeafEmbeddedPath() {
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( InvalidEmbeddedWithoutPathsCase.class );
		cfg.addAnnotatedClass( InvalidEmbeddedNonLeafCase.class );

		assertThatThrownBy( () -> cfg.build() )
				.as( "Exception should have been thrown for InvalidEmbeddedNonLeafCase having invalid path: emb.e3" )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"type '" + InvalidEmbeddedNonLeafCase.class.getName() + "'",
						"Non-matching includePaths filters: [emb.e4]",
						"Encountered field paths: [emb, emb.e1, emb.e3, emb.e3.c, emb.e3.c.indexed, emb.e3.c.notIndexed]"
				);
	}

	@Test
	void testNonIndexedPath() {
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( ReferencesC.class );
		cfg.addAnnotatedClass( PathNotIndexedCase.class );

		assertThatThrownBy( () -> cfg.build() )
				.as( "Expected search exception to contain information about invalid leaf path c.indexed, instead got error" )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"c.indexed"
				);
	}

	@Test
	void testRenamedFieldInPath() {
		cfg.addAnnotatedClass( FieldRenamedContainerEntity.class );
		cfg.addAnnotatedClass( FieldRenamedEmbeddedEntity.class );

		assertThatThrownBy( () -> cfg.build() )
				.as( "Expected search exception to contain information about invalid leaf path embedded.field, instead got error" )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"embedded.field"
				);
	}

	/**
	 * Ensures that path still marked as encountered if depth is the cause of the path being
	 * traversed, and they are the same depth
	 */
	@Test
	void testDepthMatchesPathMarkedAsEncountered() {
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
	void testDepthExceedsPathMarkedAsEncountered() {
		cfg.addAnnotatedClass( A.class );
		cfg.addAnnotatedClass( B.class );
		cfg.addAnnotatedClass( C.class );
		cfg.addAnnotatedClass( ReferencesIndexedEmbeddedA.class );
		cfg.addAnnotatedClass( DepthExceedsPathTestCase.class );
		cfg.build();
	}

}
