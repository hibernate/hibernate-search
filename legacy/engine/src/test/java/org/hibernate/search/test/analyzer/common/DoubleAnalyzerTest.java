/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.common;

import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;

/**
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-263")
@Category(SkipOnElasticsearch.class) // Custom analyzer implementations cannot be used with Elasticsearch.
public class DoubleAnalyzerTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( MyEntity.class, AlarmEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void testScopedAnalyzers() throws Exception {
		MyEntity en = new MyEntity();
		en.setId( 1 );
		en.setEntity( "anyNotNull" );
		AlarmEntity alarmEn = new AlarmEntity();
		alarmEn.setId( 2 );
		alarmEn.setProperty( "notNullAgain" );
		alarmEn.setAlarmDescription( "description" );
		helper.index( en, alarmEn );

		QueryParser parser = new QueryParser( "id", TestConstants.keywordAnalyzer );
		helper.assertThat( new MatchAllDocsQuery() )
				.matchesUnorderedIds( 1, 2 );

		helper.assertThat( parser.parse( "entity:alarm" ) )
				.from( MyEntity.class )
				.matchesExactlyIds( 1 );

		helper.assertThat( parser.parse( "property:sound" ) )
				.from( AlarmEntity.class )
				.matchesNone();

		helper.assertThat( parser.parse( "description_analyzer2:sound" ) )
				.from( AlarmEntity.class )
				.matchesExactlyIds( 2 );

		helper.assertThat( parser.parse( "description_analyzer3:music" ) )
				.from( AlarmEntity.class )
				.matchesExactlyIds( 2 );

		helper.assertThat( parser.parse( "description_normalizer1:symphony" ) )
				.from( AlarmEntity.class )
				.matchesExactlyIds( 2 );
	}
}
