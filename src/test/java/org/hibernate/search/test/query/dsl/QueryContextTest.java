package org.hibernate.search.test.query.dsl;

import org.hibernate.search.query.dsl.QueryContext;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.query.Person;

/**
 * Test Class for {@link org.hibernate.search.query.dsl.QueryContext}
 *
 * @author Navin Surtani
 */


public class QueryContextTest extends SearchTestCase {

   protected Class<?>[] getMappings() {
      Class[] clazz = new Class[1];
      clazz[0] = Person.class;
      return clazz;
   }

   public void testSameInstance(){
      QueryContext qc = new QueryContext();

      QueryContext qc2 = qc.search("findStuff");

      assert qc.equals(qc2);
   }

}
