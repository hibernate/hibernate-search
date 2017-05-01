/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.batchindexing.impl.MassIndexerImpl;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchIndexerTest {

	/**
	 * test that the MassIndexer is properly identifying the root entities
	 * from the selection of classes to be indexed.
	 */
	@Test
	public void testEntityHierarchy() {
		FullTextSessionBuilder ftsb = new FullTextSessionBuilder()
				.addAnnotatedClass( ModernBook.class )
				.addAnnotatedClass( AncientBook.class )
				.addAnnotatedClass( Dvd.class )
				.addAnnotatedClass( Book.class )
				.addAnnotatedClass( Nation.class )
				.build();
		FullTextSession fullTextSession = ftsb.openFullTextSession();
		SearchIntegrator integrator = fullTextSession.getSearchFactory().unwrap( SearchIntegrator.class );
		{
			TestableMassIndexerImpl tsii = new TestableMassIndexerImpl( integrator, Book.class );
			assertTrue( tsii.getRootEntities().contains( Book.class ) );
			assertFalse( tsii.getRootEntities().contains( ModernBook.class ) );
			assertFalse( tsii.getRootEntities().contains( AncientBook.class ) );
		}
		{
			TestableMassIndexerImpl tsii = new TestableMassIndexerImpl(
					integrator,
					ModernBook.class,
					AncientBook.class,
					Book.class
			);
			assertTrue( tsii.getRootEntities().contains( Book.class ) );
			assertFalse( tsii.getRootEntities().contains( ModernBook.class ) );
			assertFalse( tsii.getRootEntities().contains( AncientBook.class ) );
		}
		{
			TestableMassIndexerImpl tsii = new TestableMassIndexerImpl(
					integrator,
					ModernBook.class,
					AncientBook.class
			);
			assertFalse( tsii.getRootEntities().contains( Book.class ) );
			assertTrue( tsii.getRootEntities().contains( ModernBook.class ) );
			assertTrue( tsii.getRootEntities().contains( AncientBook.class ) );
		}
		//verify that indexing Object will result in one separate indexer working per root indexed entity
		{
			TestableMassIndexerImpl tsii = new TestableMassIndexerImpl( integrator, Object.class );
			assertTrue( tsii.getRootEntities().contains( Book.class ) );
			assertTrue( tsii.getRootEntities().contains( Dvd.class ) );
			assertFalse( tsii.getRootEntities().contains( AncientBook.class ) );
			assertFalse( tsii.getRootEntities().contains( Object.class ) );
			assertEquals( 2, tsii.getRootEntities().size() );
		}
		fullTextSession.close();
		ftsb.close();
	}

	private static class TestableMassIndexerImpl extends MassIndexerImpl {

		protected TestableMassIndexerImpl(SearchIntegrator integrator, Class<?>... types) {
			super( integrator, null, types );
		}

		public Set<Class<?>> getRootEntities() {
			return this.rootEntities.toPojosSet();
		}

	}


	// Test to verify that the identifier loading works even when
	// the property is not called "id"
	@Test
	@TestForIssue(jiraKey = "HSEARCH-901")
	public void testIdentifierNaming() throws InterruptedException {
		//disable automatic indexing, to test manual index creation.
		FullTextSessionBuilder ftsb = new FullTextSessionBuilder()
				.setProperty( Environment.ANALYZER_CLASS, StandardAnalyzer.class.getName() )
				.addAnnotatedClass( Dvd.class )
				.addAnnotatedClass( Nation.class )
				.addAnnotatedClass( Book.class )
				.addAnnotatedClass( WeirdlyIdentifiedEntity.class )
				.setProperty( Environment.INDEXING_STRATEGY, "manual" )
				.build();
		{
			//creating the test data in database only:
			FullTextSession fullTextSession = ftsb.openFullTextSession();
			Transaction transaction = fullTextSession.beginTransaction();
			Nation us = new Nation( "United States of America", "US" );
			fullTextSession.persist( us );
			Dvd dvda = new Dvd();
			dvda.setTitle( "Star Trek (episode 96367)" );
			dvda.setFirstPublishedIn( us );
			fullTextSession.save( dvda );
			Dvd dvdb = new Dvd();
			dvdb.setTitle( "The Trek" );
			dvdb.setFirstPublishedIn( us );
			fullTextSession.save( dvdb );
			WeirdlyIdentifiedEntity entity = new WeirdlyIdentifiedEntity();
			entity.setId( "not an identifier" );
			fullTextSession.save( entity );
			transaction.commit();
			fullTextSession.close();
		}
		{
			//verify index is still empty:
			assertEquals( 0, countResults( new TermQuery( new Term( "title", "trek" ) ), ftsb, Dvd.class ) );
			assertEquals(
					0, countResults( new TermQuery( new Term( "id", "not" ) ), ftsb, WeirdlyIdentifiedEntity.class )
			);
		}
		{
			FullTextSession fullTextSession = ftsb.openFullTextSession();
			fullTextSession.createIndexer( Dvd.class )
					.startAndWait();
			fullTextSession.close();
		}
		{
			//verify index is now containing both DVDs:
			assertEquals( 2, countResults( new TermQuery( new Term( "title", "trek" ) ), ftsb, Dvd.class ) );
		}
		{
			FullTextSession fullTextSession = ftsb.openFullTextSession();
			fullTextSession.createIndexer( WeirdlyIdentifiedEntity.class )
					.startAndWait();
			fullTextSession.close();
		}
		{
			//verify index is now containing the weirdly identified entity:
			assertEquals(
					1,
					countResults( new TermQuery( new Term( "id", "identifier" ) ), ftsb, WeirdlyIdentifiedEntity.class )
			);
		}
		ftsb.close();
	}

	@Test
	public void testExtendedIdentifierNaming() throws InterruptedException {
		//disable automatic indexing, to test manual index creation.
		FullTextSessionBuilder ftsb = new FullTextSessionBuilder()
				.setProperty( Environment.ANALYZER_CLASS, StandardAnalyzer.class.getName() )
				.addAnnotatedClass( ExtendedIssueEntity.class )
				.addAnnotatedClass( IssueEntity.class )
				.setProperty( Environment.INDEXING_STRATEGY, "manual" )
				.build();
		{
			//creating the test data in database only:
			FullTextSession fullTextSession = ftsb.openFullTextSession();
			Transaction transaction = fullTextSession.beginTransaction();
			ExtendedIssueEntity issue = new ExtendedIssueEntity();
			issue.jiraCode = "HSEARCH-977";
			issue.jiraDescription = "MassIndexer freezes when there is an indexed 'id' filed, which is not document's id";
			issue.id = 1l;
			fullTextSession.persist( issue );
			transaction.commit();
			fullTextSession.close();
		}
		{
			//verify index is still empty:
			assertEquals(
					0, countResults(
							new TermQuery( new Term( "jiraDescription", "freezes" ) ), ftsb, ExtendedIssueEntity.class
					)
			);
			assertEquals(
					0,
					countResults( new TermQuery( new Term( "jiraCode", "HSEARCH" ) ), ftsb, ExtendedIssueEntity.class )
			);
		}
		{
			FullTextSession fullTextSession = ftsb.openFullTextSession();
			fullTextSession.createIndexer( ExtendedIssueEntity.class )
					.startAndWait();
			fullTextSession.close();
		}
		{
			//verify index via term readers:
			assertEquals(
					1,
					countResults(
							new TermQuery( new Term( "jiraDescription", "freezes" ) ), ftsb, ExtendedIssueEntity.class
					)
			);

			assertEquals(
					1,
					countResults(
							NumericRangeQuery.newLongRange( "id", 1l, 1l, true, true ), ftsb, ExtendedIssueEntity.class
					)
			);
		}
		ftsb.close();
	}

	//helper method
	private int countResults(Query query, FullTextSessionBuilder ftSessionBuilder, Class<?> type) {
		FullTextSession fullTextSession = ftSessionBuilder.openFullTextSession();
		Transaction transaction = fullTextSession.beginTransaction();

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, type );
		int resultSize = fullTextQuery.getResultSize();

		transaction.commit();
		fullTextSession.close();
		return resultSize;
	}

}
