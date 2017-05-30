/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class ElasticsearchSimpleQueryStringDSLIT {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Book.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void setUp() throws Exception {
		indexTestData();
	}

	@Test(expected = SearchException.class)
	@TestForIssue(jiraKey = "HSEARCH-2678")
	public void testOverridingSeveralAnalyzers() {
		QueryBuilder qb = sfHolder.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.overridesForField( "author", "titleAnalyzer" )
				.overridesForField( "title", "authorAnalyzer" )
				.get();
		Query query = qb.simpleQueryString()
				.onFields( "title", "author" )
				.withAndAsDefaultOperator()
				.matching( "Molière" )
				.createQuery();

		HSQuery hsQuery = sfHolder.getSearchFactory().createHSQuery( query, Book.class );
		hsQuery.sort( new Sort( new SortField( "title_sort", SortField.Type.STRING ) ) );
		hsQuery.queryEntityInfos();
	}

	private void indexTestData() {
		helper.add(
				new Book( 1L, "Le chat qui regardait les étoiles", "Lilian Jackson Braun" ),
				new Book( 2L, "Le chat qui déplaçait des montagnes", "Lilian Jackson Braun" ),
				new Book( 3L, "Le Grand Molière illustré", "Caroline Guillot" ),
				new Book( 4L, "Tartuffe", "Molière" ),
				new Book( 5L, "Dom Garcie de Navarre", "moliere" ) // Molière all lowercase and without an accent
		);
	}

	@Indexed
	@Entity
	@AnalyzerDefs({
			@AnalyzerDef(name = "titleAnalyzer",
					tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
					filters = {
							@TokenFilterDef(factory = LowerCaseFilterFactory.class),
							@TokenFilterDef(factory = ASCIIFoldingFilterFactory.class)
					}
			),
			@AnalyzerDef(name = "authorAnalyzer",
					tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class)
			)
	})
	private static class Book {
		@DocumentId
		@Id
		Long id;

		@Field(analyzer = @Analyzer(definition = "titleAnalyzer"))
		@Field(name = "title_sort", analyze = Analyze.NO)
		@SortableField(forField = "title_sort")
		private String title;

		@Field(analyzer = @Analyzer(definition = "authorAnalyzer"))
		private String author;

		public Book(Long id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}
	}
}
