/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.numeric;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.Tags;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

@Tag(Tags.SKIP_ON_ELASTICSEARCH) // This test is Lucene-specific. The generic equivalent is NumericFieldTest.
class LuceneNumericFieldTest {

	@RegisterExtension
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( TouristAttraction.class, ScoreBoard.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@BeforeEach
	void setUp() {
		prepareData();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1987")
	void testDocumentFieldIsNumeric() {
		List<Object[]> list = (List<Object[]>) helper.hsQuery( TouristAttraction.class )
				.projection( ProjectionConstants.DOCUMENT )
				.fetch();

		assertThat( list ).hasSize( 1 );
		Document document = (Document) list.iterator().next()[0];

		IndexableField scoreNumeric = document.getField( "scoreNumeric" );
		assertThat( scoreNumeric.numericValue() ).isEqualTo( 23 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1987")
	void testNumericMappingOfEmbeddedFields() {
		List<Object[]> list = (List<Object[]>) helper.hsQuery( ScoreBoard.class )
				.projection( ProjectionConstants.DOCUMENT )
				.fetch();

		assertThat( list ).hasSize( 1 );
		Document document = (Document) list.iterator().next()[0];

		IndexableField scoreNumeric = document.getField( "score_id" );
		assertThat( scoreNumeric.numericValue() ).isEqualTo( 1 );

		IndexableField beta = document.getField( "score_beta" );
		assertThat( beta.numericValue() ).isEqualTo( 100 );
	}

	private void prepareData() {
		TouristAttraction attraction = new TouristAttraction( 1, (short) 23, (short) 46L );
		helper.add( attraction );

		Score score1 = new Score();
		score1.id = 1;
		score1.subscore = 100;

		ScoreBoard scoreboard = new ScoreBoard();
		scoreboard.id = 1L;
		scoreboard.scores.add( score1 );
		helper.add( scoreboard );
	}

	@Indexed
	private static class ScoreBoard {

		@DocumentId
		Long id;

		@IndexedEmbedded(includeEmbeddedObjectId = true, prefix = "score_")
		Set<Score> scores = new HashSet<Score>();

	}

	private static class Score {
		@DocumentId
		@NumericField
		Integer id;

		@Field(name = "beta", store = Store.YES)
		Integer subscore;
	}
}
