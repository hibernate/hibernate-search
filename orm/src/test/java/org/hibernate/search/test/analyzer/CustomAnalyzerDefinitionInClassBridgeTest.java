/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Equivalent of {@link CustomAnalyzerImplementationInClassBridgeTest}, but using analyzer
 * definitions instead of custom analyzer implementations.
 *
 * @author Hardy Ferentschik
 */
public class CustomAnalyzerDefinitionInClassBridgeTest extends SearchTestBase {

	public static final Log log = LoggerFactory.make();

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { FooBar.class };
	}

	@Test
	public void testClassBridgeWithSingleField() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.persist( new FooBar() );
		tx.commit();
		fullTextSession.clear();

		TermQuery termQuery = new TermQuery( new Term( "classField", "dog" ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( termQuery );

		assertEquals(
				"custom analyzer should have inserted search token",
				1,
				query.getResultSize()
		);

		fullTextSession.close();
	}

	@Entity
	@Table(name = "JUSTFOOBAR")
	@Indexed
	@AnalyzerDef(
			name = "foobarAnalyzer",
			tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
			filters = @TokenFilterDef(factory = LowerCaseFilterFactory.class)
	)
	@ClassBridge(name = "classField", impl = FooBarBridge.class, analyzer = @Analyzer(definition = "foobarAnalyzer"))
	public static class FooBar {
		@Id
		@GeneratedValue
		private Integer id;
	}

	public static class FooBarBridge implements FieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name, "alarm Dog performance", document );
		}
	}
}
