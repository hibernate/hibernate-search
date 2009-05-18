package org.hibernate.search.test.batchindexing;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.impl.IndexerImpl;
import org.hibernate.search.test.util.FullTextSessionBuilder;

public class SearchIndexerTest extends TestCase {
	
	public void testEntityHierarchy() {
		FullTextSessionBuilder ftsb = new FullTextSessionBuilder()
			.addAnnotatedClass( ModernBook.class )
			.addAnnotatedClass( AncientBook.class )
			.addAnnotatedClass( Dvd.class )
			.addAnnotatedClass( Book.class )
			.build();
		FullTextSession fullTextSession = ftsb.openFullTextSession();
		SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();
		{
			TestableSearchIndexerImpl tsii = new TestableSearchIndexerImpl( searchFactory, Book.class );
			assertTrue( tsii.getRootEntities().contains( Book.class ) );
			assertFalse( tsii.getRootEntities().contains( ModernBook.class ) );
			assertFalse( tsii.getRootEntities().contains( AncientBook.class ) );
		}
		{
			TestableSearchIndexerImpl tsii = new TestableSearchIndexerImpl( searchFactory, ModernBook.class, AncientBook.class, Book.class );
			assertTrue( tsii.getRootEntities().contains( Book.class ) );
			assertFalse( tsii.getRootEntities().contains( ModernBook.class ) );
			assertFalse( tsii.getRootEntities().contains( AncientBook.class ) );
		}
		{
			TestableSearchIndexerImpl tsii = new TestableSearchIndexerImpl( searchFactory, ModernBook.class, AncientBook.class );
			assertFalse( tsii.getRootEntities().contains( Book.class ) );
			assertTrue( tsii.getRootEntities().contains( ModernBook.class ) );
			assertTrue( tsii.getRootEntities().contains( AncientBook.class ) );
		}
		//verify that indexing Object will result in one separate indexer working per root indexed entity
		{
			TestableSearchIndexerImpl tsii = new TestableSearchIndexerImpl( searchFactory, Object.class );
			assertTrue( tsii.getRootEntities().contains( Book.class ) );
			assertTrue( tsii.getRootEntities().contains( Dvd.class ) );
			assertFalse( tsii.getRootEntities().contains( AncientBook.class ) );
			assertFalse( tsii.getRootEntities().contains( Object.class ) );
			assertEquals( 2, tsii.getRootEntities().size() );
		}
	}
	
	private class TestableSearchIndexerImpl extends IndexerImpl {
		
		protected TestableSearchIndexerImpl(SearchFactoryImplementor searchFactory, Class<?>... types) {
			super( searchFactory, null, types );
		}

		public Set<Class<?>> getRootEntities() {
			return this.rootEntities;
		}
		
	}
	
	public void testIdentifierNaming() throws InterruptedException {
		//disable automatic indexing, to test manual index creation.
		FullTextSessionBuilder ftsb = new FullTextSessionBuilder()
			.setProperty( org.hibernate.search.Environment.ANALYZER_CLASS, StandardAnalyzer.class.getName() )
			.addAnnotatedClass( Dvd.class )
			.setProperty( Environment.INDEXING_STRATEGY, "manual" )
			.build();
		{
			//creating the test data in database only:
			FullTextSession fullTextSession = ftsb.openFullTextSession();
			Transaction transaction = fullTextSession.beginTransaction();
			Dvd dvda = new Dvd();
			dvda.setTitle( "Star Trek (episode 96367)" );
			fullTextSession.save(dvda);
			Dvd dvdb = new Dvd();
			dvdb.setTitle( "The Trek" );
			fullTextSession.save(dvdb);
			transaction.commit();
			fullTextSession.close();
		}
		{	
			//verify index is still empty:
			assertEquals( 0, countResults( new Term( "title", "trek" ), ftsb, Dvd.class ) );
		}
		{
			FullTextSession fullTextSession = ftsb.openFullTextSession();
			fullTextSession.createIndexer( Dvd.class )
				.startAndWait( 10, TimeUnit.SECONDS );
			fullTextSession.close();
		}
		{	
			//verify index is now containing both DVDs:
			assertEquals( 2, countResults( new Term( "title", "trek" ), ftsb, Dvd.class ) );
		}
	}
	
	public int countResults( Term termForQuery, FullTextSessionBuilder ftSessionBuilder, Class<?> type ) {
		TermQuery fullTextQuery = new TermQuery( new Term( "title", "trek" ) );
		FullTextSession fullTextSession = ftSessionBuilder.openFullTextSession();
		Transaction transaction = fullTextSession.beginTransaction();
		FullTextQuery query = fullTextSession.createFullTextQuery( fullTextQuery, type );
		int resultSize = query.getResultSize();
		transaction.commit();
		fullTextSession.close();
		return resultSize;
	}

}
