package org.hibernate.search.test;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import java.util.List;
import org.jboss.cache.Cache;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.search.CacheQuery;
import org.jboss.cache.search.QueryResultIterator;
import org.jboss.cache.search.SearchableCache;
import org.jboss.cache.search.SearchableCacheFactory;

import java.io.File;

/**
 * @author Navin Surtani  - navin@surtani.org
 *
 *
 * Creates an instance of JBoss Cache Searchable and then searches through it. Creates instances of org.hibernate.search.test.Person.
 *
 */

public class LocalCacheTest extends junit.framework.TestCase
{
   SearchableCache searchableCache;
   Person person1;
   Person person2;
   Person person3;
   Person person4;
   Person person5;
   QueryParser queryParser;
   Query luceneQuery;
   CacheQuery cacheQuery;
   List found;
   String key1 = "Navin";
   String key2 = "BigGoat";
   String key3 = "MiniGoat";

   public void setUp()
   {

      //Create a searchable cache instance.
      Cache coreCache = new DefaultCacheFactory().createCache();
      searchableCache = new SearchableCacheFactory().createSearchableCache(coreCache, Person.class);

      //Creates 3 person objects.
      person1 = new Person();
      person1.setName("Navin Surtani");
      person1.setBlurb("Likes playing WoW");

      person2 = new Person();
      person2.setName("Big Goat");
      person2.setBlurb("Eats grass");

      person3 = new Person();
      person3.setName("Mini Goat");
      person3.setBlurb("Eats cheese");

      person5 = new Person();
      person5.setName("Smelly Cat");
      person5.setBlurb("Eats fish");

      //Put the 3 created objects in the searchableCache.
      searchableCache.put(Fqn.fromString("/a/b/c"), key1, person1);
      searchableCache.put(Fqn.fromString("/a/b/d"), key2, person2);
      searchableCache.put(Fqn.fromString("/a/b/c"), key3, person3);

   }

   public void tearDown()
   {
      if (searchableCache != null) searchableCache.stop();
       cleanUpIndexes();
   }

   public void testSimple() throws ParseException
   {
      queryParser = new QueryParser("blurb", new StandardAnalyzer());
      luceneQuery = queryParser.parse("playing");

      // As with JBoss Cache Searchable, we create a CacheQuery instance as opposed to a FullTextQuery.
      cacheQuery = searchableCache.createQuery(luceneQuery);

      found = cacheQuery.list();

      assert found.size() == 1;
      assert found.get(0).equals(person1);
   }

   public void testSimpleIterator() throws ParseException
   {
      queryParser = new QueryParser("blurb", new StandardAnalyzer());
      luceneQuery = queryParser.parse("playing");
      cacheQuery = searchableCache.createQuery(luceneQuery);

      QueryResultIterator found = cacheQuery.iterator();

      assert found.isFirst();
      assert found.isLast();
   }


   public void testMultipleResults() throws ParseException
   {

      queryParser = new QueryParser("name", new StandardAnalyzer());

      luceneQuery = queryParser.parse("goat");
      cacheQuery = searchableCache.createQuery(luceneQuery);
      found = cacheQuery.list();

      assert found.size() == 2;
      assert found.get(0) == person2;
      assert found.get(1) == person3;

   }

   public void testModified() throws ParseException
   {
      queryParser = new QueryParser("blurb", new StandardAnalyzer());
      luceneQuery = queryParser.parse("playing");
      cacheQuery = searchableCache.createQuery(luceneQuery);

      found = cacheQuery.list();

      assert found.size() == 1;
      assert found.get(0).equals(person1);

      person1.setBlurb("Likes pizza");
      searchableCache.put(Fqn.fromString("/a/b/c/"), key1, person1);

      queryParser = new QueryParser("blurb", new StandardAnalyzer());
      luceneQuery = queryParser.parse("pizza");
      cacheQuery = searchableCache.createQuery(luceneQuery);

      found = cacheQuery.list();

      assert found.size() == 1;
      assert found.get(0).equals(person1);
   }

   public void testAdded() throws ParseException
   {
      queryParser = new QueryParser("name", new StandardAnalyzer());

      luceneQuery = queryParser.parse("Goat");
      cacheQuery = searchableCache.createQuery(luceneQuery);
      found = cacheQuery.list();

      assert found.size() == 2 : "Size of list should be 2";
      assert found.contains(person2);
      assert found.contains(person3);
      assert !found.contains(person4) : "This should not contain object person4";

      person4 = new Person();
      person4.setName("Mighty Goat");
      person4.setBlurb("Also eats grass");

      searchableCache.put(Fqn.fromString("/r/a/m/"), "Ram", person4);

      luceneQuery = queryParser.parse("Goat");
      cacheQuery = searchableCache.createQuery(luceneQuery);
      found = cacheQuery.list();

      assert found.size() == 3 : "Size of list should be 3";
      assert found.contains(person2);
      assert found.contains(person3);
      assert found.contains(person4) : "This should now contain object person4";
   }

   public void testRemoved() throws ParseException
   {
      queryParser = new QueryParser("name", new StandardAnalyzer());

      luceneQuery = queryParser.parse("Goat");
      cacheQuery = searchableCache.createQuery(luceneQuery);
      found = cacheQuery.list();

      assert found.size() == 2;
      assert found.contains(person2);
      assert found.contains(person3) : "This should still contain object person3";

      searchableCache.remove(Fqn.fromString("/a/b/c/"), key3);

      luceneQuery = queryParser.parse("Goat");
      cacheQuery = searchableCache.createQuery(luceneQuery);
      found = cacheQuery.list();

      assert found.size() == 1;
      assert found.contains(person2);
      assert !found.contains(person3) : "The search should not return person3";


   }

   public void testSetSort() throws ParseException
   {
      person2.setAge(35);
      person3.setAge(12);

      Sort sort = new Sort ("age");

      queryParser = new QueryParser("name", new StandardAnalyzer());

      luceneQuery = queryParser.parse("Goat");
      cacheQuery = searchableCache.createQuery(luceneQuery);
      found = cacheQuery.list();

      assert found.size() == 2;

      cacheQuery.setSort(sort);

      found = cacheQuery.list();

      assert found.size() == 2;
      assert found.get(0).equals(person2);
      assert found.get(1).equals(person3);
   }

   public void testSetFilter() throws ParseException
   {
      queryParser = new QueryParser("blurb", new StandardAnalyzer());
      luceneQuery = queryParser.parse("goat");
      Filter filter = new QueryWrapperFilter(luceneQuery);

      cacheQuery = searchableCache.createQuery(luceneQuery);

      cacheQuery.setFilter(filter);
   }


   private static void cleanUpIndexes()
   {
      Class[] knownClasses = {Person.class};
      for (Class c : knownClasses)
      {
         String dirName = c.getName();
         File file = new File(dirName);
         if (file.exists())
         {
            recursiveDelete(file);
         }
      }
   }

   private static void recursiveDelete(File f)
   {
      if (f.isDirectory())
      {
         File[] files = f.listFiles();
         for (File file : files) recursiveDelete(file);
      }
      else
      {
         f.delete();
      }
   }


}
