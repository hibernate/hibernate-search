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

import java.util.List;

/**
 * @author Navin Surtani (<a href="mailto:nsurtani@redhat.com">nsurtani@redhat.com</a>)
 */
public class TestProvidedId extends SearchTestCase {

   JBossCachePerson person1, person2, person3, person4;

   protected Class[] getMappings() {
      return new Class[]{
              JBossCachePerson.class
      };
   }

   public void testProvidedId() throws ParseException {

      person1 = new JBossCachePerson();
      person1.setName("Navin Surtani");
      person1.setBlurb("Likes playing WoW");

      person2 = new JBossCachePerson();
      person2.setName("Big Goat");
      person2.setBlurb("Eats grass");

      person3 = new JBossCachePerson();
      person3.setName("Mini Goat");
      person3.setBlurb("Eats cheese");

      person4 = new JBossCachePerson();
      person4.setName("Smelly Cat");
      person4.setBlurb("Eats fish");


      Session session = openSession();
      FullTextSession fullTextSession = Search.getFullTextSession(session);
      Transaction transaction = fullTextSession.beginTransaction();
      session.save(person1);
      session.save(person2);
      session.save(person3);
      session.save(person4);

      QueryParser parser = new QueryParser("Name", new StandardAnalyzer());
      Query luceneQuery = parser.parse("Goat");

      FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(luceneQuery, JBossCachePerson.class);


      List results = fullTextQuery.list();

      transaction.commit();
      session.close();

      System.out.println("result size is " + results.size());
   }


}
