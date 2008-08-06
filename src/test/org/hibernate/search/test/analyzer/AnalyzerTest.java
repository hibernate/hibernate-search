// $Id$
package org.hibernate.search.test.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.AnalyzerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class AnalyzerTest extends SearchTestCase {

	public static final Logger log = LoggerFactory.getLogger(AnalyzerTest.class);

	public void testScopedAnalyzers() throws Exception {
		MyEntity en = new MyEntity();
		en.setEntity("Entity");
		en.setField("Field");
		en.setProperty("Property");
		en.setComponent(new MyComponent());
		en.getComponent().setComponentProperty("component property");
		FullTextSession s = Search.getFullTextSession(openSession());
		Transaction tx = s.beginTransaction();
		s.persist(en);
		tx.commit();

		tx = s.beginTransaction();
		QueryParser parser = new QueryParser("id", new StandardAnalyzer());
		org.apache.lucene.search.Query luceneQuery = parser.parse("entity:alarm");
		FullTextQuery query = s.createFullTextQuery(luceneQuery, MyEntity.class);
		assertEquals(1, query.getResultSize());

		luceneQuery = parser.parse("property:cat");
		query = s.createFullTextQuery(luceneQuery, MyEntity.class);
		assertEquals(1, query.getResultSize());

		luceneQuery = parser.parse("field:energy");
		query = s.createFullTextQuery(luceneQuery, MyEntity.class);
		assertEquals(1, query.getResultSize());

		luceneQuery = parser.parse("component.componentProperty:noise");
		query = s.createFullTextQuery(luceneQuery, MyEntity.class);
		assertEquals(1, query.getResultSize());

		s.delete(query.uniqueResult());
		tx.commit();

		s.close();
	}

	public void testScopedAnalyzersFromSearchFactory() throws Exception {
		FullTextSession session = Search.getFullTextSession(openSession());
		SearchFactory searchFactory = session.getSearchFactory();
		Analyzer analyzer = searchFactory.getAnalyzer(MyEntity.class);

		// you can pass what so ever into the analysis since the used analyzers are
		// returning the same tokens all the time. We just want to make sure that
		// the right analyzers are used.
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis(analyzer, "entity", "");
		AnalyzerUtils.assertTokensEqual(tokens, new String[] { "alarm", "dog", "performance" });

		tokens = AnalyzerUtils.tokensFromAnalysis(analyzer, "property", "");
		AnalyzerUtils.assertTokensEqual(tokens, new String[] { "sound", "cat", "speed" });

		tokens = AnalyzerUtils.tokensFromAnalysis(analyzer, "field", "");
		AnalyzerUtils.assertTokensEqual(tokens, new String[] { "music", "elephant", "energy" });

		tokens = AnalyzerUtils.tokensFromAnalysis(analyzer, "component.componentProperty", "");
		AnalyzerUtils.assertTokensEqual(tokens, new String[] { "noise", "mouse", "light" });

		// test border cases
		try {
			searchFactory.getAnalyzer((Class) null);
		} catch ( IllegalArgumentException iae ) {
			log.debug("success");
		}

		try {
			searchFactory.getAnalyzer(String.class);
		} catch ( IllegalArgumentException iae ) {
			log.debug("success");
		}

		session.close();
	}

	protected Class[] getMappings() {
		return new Class[] { MyEntity.class };
	}
}
