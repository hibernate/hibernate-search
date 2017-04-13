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
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
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
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class ElasticsearchSimpleQueryStringDSLIT extends SearchTestBase {

	private FullTextSession fullTextSession;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		indexTestData();
	}

	@Test(expected = SearchException.class)
	@TestForIssue(jiraKey = "HSEARCH-2678")
	public void testOverridingSeveralAnalyzers() {
		Transaction transaction = fullTextSession.beginTransaction();

		try {
			QueryBuilder qb = fullTextSession.getSearchFactory()
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

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Book.class );
			fullTextQuery.setSort( new Sort( new SortField( "title", SortField.Type.STRING ) ) );
			fullTextQuery.getResultList();
		}
		finally {
			transaction.commit();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Book.class
		};
	}

	private void indexTestData() {
		Transaction tx = fullTextSession.beginTransaction();

		fullTextSession.persist( new Book( 1L, "Le chat qui regardait les étoiles", "Lilian Jackson Braun" ) );
		fullTextSession.persist( new Book( 2L, "Le chat qui déplaçait des montagnes", "Lilian Jackson Braun" ) );
		fullTextSession.persist( new Book( 3L, "Le Grand Molière illustré", "Caroline Guillot" ) );
		fullTextSession.persist( new Book( 4L, "Tartuffe", "Molière" ) );
		fullTextSession.persist( new Book( 5L, "Dom Garcie de Navarre", "moliere" ) ); // Molière all lowercase and without an accent

		tx.commit();
		fullTextSession.clear();
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
	public class Book {

		@DocumentId
		@Id
		Long id;

		@Field(analyzer = @Analyzer(definition = "titleAnalyzer"))
		@SortableField
		private String title;

		@Field(analyzer = @Analyzer(definition = "authorAnalyzer"))
		private String author;

		public Book() {
		}

		public Book(Long id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}
}
