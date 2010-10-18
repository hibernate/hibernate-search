package org.hibernate.search.test.engine;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.util.NumericFieldUtils;
import org.hibernate.search.test.SearchTestCase;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;


public class NumericFieldTest extends SearchTestCase {


	private List numericQueryFor(String fieldName, FullTextSession session, Object from, Object to) {
		Query query = NumericFieldUtils.createNumericRangeQuery(fieldName, from, to, true, true);
		return session.createFullTextQuery(query, Location.class).list();
	}

	public void testIndexAndSearchNumericField() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.save(new Location(245, 3L, 0.5510d, 56.12d, 34, "Random text", 45.56d));
		tx.commit();
		session.clear();

		FullTextSession fts = Search.getFullTextSession(session);
		tx = session.beginTransaction();

		assertEquals("Query id ", 1, numericQueryFor( "id",fts,200,250 ).size() );
		assertEquals("Query by double range", 1, numericQueryFor( "latitude",fts,0.55d,0.552d ).size() );
		assertEquals("Query by integer range", 1, numericQueryFor("ranking",fts,30,40).size() );
		assertEquals("Query by long range", 1, numericQueryFor("myCounter",fts,1L,5L).size() );
		assertEquals("Query by double multiple", 1, numericQueryFor("strMultiple",fts,45.4d, 45.6d).size() );

		TermQuery termQuery = new TermQuery(new Term("description", "text"));

		List result = fts.createFullTextQuery(termQuery, Location.class).list();
		assertEquals(1, result.size());

		tx.commit();
		fts.close();

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Location.class};
	}

}