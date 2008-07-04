// $Id$
package org.hibernate.search.test.jpa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

/**
 * Serialization test for entity manager. HSEARCH-117.
 * 
 * @author Hardy Ferentschik
 */
public class EntityManagerSerializationTest extends JPATestCase {

	/**
	 * Test that a entity manager can successfully be serialized and
	 * deserialized.
	 * 
	 * @throws Exception
	 *             in case the test fails.
	 */
	public void testSerialization() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager(factory
				.createEntityManager());

		indexSearchAssert(em);
		
		File tmpFile = File.createTempFile("entityManager", "ser", null);
		serializeEM(em, tmpFile);
		em = deserializeEM(tmpFile);
		
		indexSearchAssert(em);
		
		em.close();
		
		// cleanup
		tmpFile.delete();
	}

	private FullTextEntityManager deserializeEM(File tmpFile) throws ClassNotFoundException {
		FullTextEntityManager em = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(tmpFile);
			in = new ObjectInputStream(fis);
			em = (FullTextEntityManager) in.readObject();
			in.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
			fail();
		}
		return em;
	}

	private void serializeEM(FullTextEntityManager em, File tmpFile) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(tmpFile);
			out = new ObjectOutputStream(fos);
			out.writeObject(em);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			fail();
		}
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] { Bretzel.class };
	}

	/**
	 * Helper method for testing the entity manager before and after
	 * serialization.
	 * 
	 * @param em
	 * @throws Exception
	 */
	private void indexSearchAssert(FullTextEntityManager em) throws Exception {
		em.getTransaction().begin();
		Bretzel bretzel = new Bretzel(23, 34);
		em.persist(bretzel);
		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();
		QueryParser parser = new QueryParser("title", new StopAnalyzer());
		Query query = parser.parse("saltQty:noword");
		assertEquals(0, em.createFullTextQuery(query).getResultList().size());
		query = new TermQuery(new Term("saltQty", "23.0"));
		assertEquals("getResultList", 1, em.createFullTextQuery(query)
				.getResultList().size());
		assertEquals("getSingleResult and object retrieval", 23f, ((Bretzel) em
				.createFullTextQuery(query).getSingleResult()).getSaltQty());
		assertEquals(1, em.createFullTextQuery(query).getResultSize());
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		em.remove(em.find(Bretzel.class, bretzel.getId()));
		em.getTransaction().commit();
	}
}
