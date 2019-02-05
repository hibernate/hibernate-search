/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.multiindex;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MultiIndexCompatibilitySearchIT {

	private static final String BOOK_INDEX_NAME = "Book";
	private static final String VIDEO_INDEX_NAME = "Video";
	public static final String BOOK_1 = "book1";
	public static final String VIDEO_1 = "video1";

	private static final String FULLTEXT_FIELD_NAME = "fullText";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private FullTextFieldIndexAccessors bookIndexAccessors;
	private StubMappingIndexManager bookIndexManager;

	private FullTextFieldIndexAccessors videoIndexAccessors;
	private StubMappingIndexManager videoIndexManager;

	@Before
	public void before() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						BOOK_INDEX_NAME, BOOK_INDEX_NAME,
						ctx -> this.bookIndexAccessors = new FullTextFieldIndexAccessors( ctx.getSchemaElement() ),
						indexMapping -> this.bookIndexManager = indexMapping
				)
				.withIndex(
						VIDEO_INDEX_NAME, VIDEO_INDEX_NAME,
						ctx -> this.videoIndexAccessors = new FullTextFieldIndexAccessors( ctx.getSchemaElement() ),
						indexMapping -> this.videoIndexManager = indexMapping
				)
				.setup();

		initData();
	}

	@Test
	public void verifyCompatibilityOnFullTextField() {
		StubMappingSearchTarget searchTarget = videoIndexManager.createSearchTarget( bookIndexManager );
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.bool( b ->
						b.must( f.match()
								.onField( FULLTEXT_FIELD_NAME ).boostedTo( 2.0f )
								.matching( "Java" )
						)
				) )
				.build();

		SearchResultAssert.assertThat( query ).hasDocRefHitsAnyOrder( c -> {
			c.doc( BOOK_INDEX_NAME, BOOK_1 );
			c.doc( VIDEO_INDEX_NAME, VIDEO_1 );
		} );
	}

	private void initData() {
		// add 1 book in Index Book
		IndexWorkPlan<? extends DocumentElement> workPlan = bookIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( BOOK_1 ), document -> bookIndexAccessors.fullText.write( document, "Effective Java" ) );
		workPlan.execute().join();

		// add 1 video in Index Video
		workPlan = videoIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( VIDEO_1 ), document -> videoIndexAccessors.fullText.write( document, "Java EE - The Competitive Advantage For Startups" ) );
		workPlan.execute().join();
	}

	private static class FullTextFieldIndexAccessors {
		final IndexFieldAccessor<String> fullText;

		FullTextFieldIndexAccessors(IndexSchemaElement root) {
			fullText = root.field( FULLTEXT_FIELD_NAME, f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name ).toIndexFieldType() )
					.createAccessor();
		}
	}

}
