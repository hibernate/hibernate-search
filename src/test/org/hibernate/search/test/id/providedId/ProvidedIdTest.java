package org.hibernate.search.test.id.providedId;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Navin Surtani (<a href="mailto:nsurtani@redhat.com">nsurtani@redhat.com</a>)
 */
public class ProvidedIdTest extends SearchTestCase {

   JBossCachePerson person1, person2;

   protected Class[] getMappings() {
      return new Class[]{
              JBossCachePerson.class
      };
   }

   public void testProvidedId() throws ParseException {

      person1 = new JBossCachePerson();
      person1.setName("Big Goat");
      person1.setBlurb("Eats grass");

      person2 = new JBossCachePerson();
      person2.setName("Mini Goat");
      person2.setBlurb("Eats cheese");


      Session session = openSession();
      FullTextSession fullTextSession = Search.getFullTextSession(session);
      Transaction transaction = session.beginTransaction();
      session.persist(person1);
      session.persist(person2);

      transaction.commit();
      session.clear();

      transaction = fullTextSession.beginTransaction();
      
      QueryParser parser = new QueryParser("name", new StandardAnalyzer());
      Query luceneQuery = parser.parse("Goat");

      FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(luceneQuery, JBossCachePerson.class);


      List results = fullTextQuery.list();

      transaction.commit();
      session.close();

      assert results.size() == 2;
   }


}
