/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package example;

import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.Session;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class IndexAndSearchTest {

    private EntityManagerFactory emf;

    private EntityManager em;

    private static Logger log = LoggerFactory.getLogger(IndexAndSearchTest.class);

    @Before
    public void setUp() {
        initHibernate();
    }

    @After
    public void tearDown() {
        purge();
    }

    @Test
    public void testIndexAndSearch() throws Exception {
        List<Book> books = search("hibernate");
        assertEquals("Should get empty list since nothing is indexed yet", 0, books.size());

        index();

        // search by title
        books = search("hibernate");
        assertEquals("Should find one book", 1, books.size());
        assertEquals("Wrong title", "Java Persistence with Hibernate", books.get(0).getTitle());

        // search author
        books = search("\"Gavin King\"");
        assertEquals("Should find one book", 1, books.size());
        assertEquals("Wrong title", "Java Persistence with Hibernate", books.get(0).getTitle());
    }

    @Test
    public void testStemming() throws Exception {

        index();

        List<Book> books = search("refactor");
        assertEquals("Wrong title", "Refactoring: Improving the Design of Existing Code", books.get(0).getTitle());

        books = search("refactors");
        assertEquals("Wrong title", "Refactoring: Improving the Design of Existing Code", books.get(0).getTitle());

        books = search("refactored");
        assertEquals("Wrong title", "Refactoring: Improving the Design of Existing Code", books.get(0).getTitle());

        books = search("refactoring");
        assertEquals("Wrong title", "Refactoring: Improving the Design of Existing Code", books.get(0).getTitle());
    }


    private void initHibernate() {
        Ejb3Configuration config = new Ejb3Configuration();
        config.configure("hibernate-search-example", new HashMap());
        emf = config.buildEntityManagerFactory();
        em = emf.createEntityManager();
    }

    private void index() {
        FullTextSession ftSession = org.hibernate.search.Search.getFullTextSession((Session) em.getDelegate());
        List results = ftSession.createCriteria(Book.class).list();
        for (Object obj : results) {
            ftSession.index(obj);
        }
    }

    private void purge() {
        FullTextSession ftSession = org.hibernate.search.Search.getFullTextSession((Session) em.getDelegate());
        ftSession.purgeAll(Book.class);
    }

    private List<Book> search(String searchQuery) throws ParseException {
        Query query = searchQuery(searchQuery);

        List<Book> books = query.getResultList();

        for (Book b : books) {
            log.info("Title: " + b.getTitle());
        }
        return books;
    }

    private Query searchQuery(String searchQuery) throws ParseException {

        String[] bookFields = {"title", "subtitle", "authors.name", "publicationDate"};

        //lucene part
        Map<String, Float> boostPerField = new HashMap<String, Float>(4);
        boostPerField.put(bookFields[0], (float) 4);
        boostPerField.put(bookFields[1], (float) 3);
        boostPerField.put(bookFields[2], (float) 4);
        boostPerField.put(bookFields[3], (float) .5);

        FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager((EntityManager) em);

        QueryParser parser = new MultiFieldQueryParser(bookFields, ftEm.getSearchFactory().getAnalyzer("customanalyzer"),
                boostPerField);

        org.apache.lucene.search.Query luceneQuery;
        luceneQuery = parser.parse(searchQuery);

        final FullTextQuery query = ftEm.createFullTextQuery(luceneQuery, Book.class);

        return query;
    }

}
