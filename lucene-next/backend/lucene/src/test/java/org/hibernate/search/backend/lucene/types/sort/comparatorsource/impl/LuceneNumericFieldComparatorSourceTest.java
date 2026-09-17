/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Pruning;

@TestForIssue(jiraKey = "HSEARCH-5014")
class LuceneNumericFieldComparatorSourceTest {

	@Test
	void singleValuedInRoot_noNestedDocumentPath() {
		assertThat( source( true, null ).effectivePruning( Pruning.GREATER_THAN ) )
				.isEqualTo( Pruning.GREATER_THAN );
		assertThat( source( true, null ).effectivePruning( Pruning.GREATER_THAN_OR_EQUAL_TO ) )
				.isEqualTo( Pruning.GREATER_THAN_OR_EQUAL_TO );
	}

	@Test
	void singleValuedInRoot_nestedDocumentPath() {
		assertThat( source( true, "nested" ).effectivePruning( Pruning.GREATER_THAN ) )
				.isEqualTo( Pruning.NONE );
	}

	@Test
	void multiValuedInRoot_noNestedDocumentPath() {
		assertThat( source( false, null ).effectivePruning( Pruning.GREATER_THAN ) )
				.isEqualTo( Pruning.NONE );
	}

	@Test
	void multiValuedInRoot_nestedDocumentPath() {
		assertThat( source( false, "nested" ).effectivePruning( Pruning.GREATER_THAN ) )
				.isEqualTo( Pruning.NONE );
	}

	@Test
	void none_staysNone() {
		assertThat( source( true, null ).effectivePruning( Pruning.NONE ) )
				.isEqualTo( Pruning.NONE );
	}

	private static LuceneNumericFieldComparatorSource<Long> source(boolean singleValuedInRoot,
			String nestedDocumentPath) {
		return new LuceneNumericFieldComparatorSource<>( nestedDocumentPath, null, null, null, null,
				singleValuedInRoot );
	}
}
