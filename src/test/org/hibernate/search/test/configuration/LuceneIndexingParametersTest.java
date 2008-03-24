package org.hibernate.search.test.configuration;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.impl.SearchFactoryImpl;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;

/**
 * @author Sanne Grinovero
 */
public class LuceneIndexingParametersTest extends SearchTestCase {
	
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		
		//super sets:
		//cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
		//cfg.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
		
		cfg.setProperty( "hibernate.search.default.batch.ram_buffer_size", "1" );
		
		cfg.setProperty( "hibernate.search.default.transaction.ram_buffer_size", "2" );
		cfg.setProperty( "hibernate.search.default.transaction.max_merge_docs", "9" );
		cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "10" );
		cfg.setProperty( "hibernate.search.default.transaction.max_buffered_docs", "11" );
		
		cfg.setProperty( "hibernate.search.Book.batch.ram_buffer_size", "3" );
		cfg.setProperty( "hibernate.search.Book.batch.max_merge_docs", "12" );
		cfg.setProperty( "hibernate.search.Book.batch.merge_factor", "13" );
		cfg.setProperty( "hibernate.search.Book.batch.max_buffered_docs", "14" );
		
		cfg.setProperty( "hibernate.search.Book.transaction.ram_buffer_size", "4" );
		cfg.setProperty( "hibernate.search.Book.transaction.max_merge_docs", "15" );
		cfg.setProperty( "hibernate.search.Book.transaction.merge_factor", "16" );
		cfg.setProperty( "hibernate.search.Book.transaction.max_buffered_docs", "17" );
		
		cfg.setProperty( "hibernate.search.Documents.ram_buffer_size", "4" );
		
	}
	
	public void testUnsetBatchValueTakesTransaction() throws Exception {
		FullTextSession fullTextSession = Search.createFullTextSession( openSession() );
		SearchFactoryImpl searchFactory = (SearchFactoryImpl) fullTextSession.getSearchFactory();
		LuceneIndexingParameters indexingParameters = searchFactory.getIndexingParameters(searchFactory.getDirectoryProviders(Document.class)[0]);
		assertEquals(10, (int)indexingParameters.getBatchIndexParameters().getMergeFactor());
		assertEquals(1000, (int)indexingParameters.getBatchIndexParameters().getMaxBufferedDocs());
		fullTextSession.close();
	}
	
	public void testBatchParametersDefault() throws Exception {
		FullTextSession fullTextSession = Search.createFullTextSession( openSession() );
		SearchFactoryImpl searchFactory = (SearchFactoryImpl) fullTextSession.getSearchFactory();
		LuceneIndexingParameters indexingParameters = searchFactory.getIndexingParameters(searchFactory.getDirectoryProviders(Author.class)[0]);
		assertEquals(1, (int)indexingParameters.getBatchIndexParameters().getRamBufferSizeMB());
		assertEquals(9, (int)indexingParameters.getBatchIndexParameters().getMaxMergeDocs());
		assertEquals(1000, (int)indexingParameters.getBatchIndexParameters().getMaxBufferedDocs());
		assertEquals(10, (int)indexingParameters.getBatchIndexParameters().getMergeFactor());
		fullTextSession.close();
	}
	
	public void testTransactionParametersDefault() throws Exception {
		FullTextSession fullTextSession = Search.createFullTextSession( openSession() );
		SearchFactoryImpl searchFactory = (SearchFactoryImpl) fullTextSession.getSearchFactory();
		LuceneIndexingParameters indexingParameters = searchFactory.getIndexingParameters(searchFactory.getDirectoryProviders(Author.class)[0]);
		assertEquals(2, (int)indexingParameters.getTransactionIndexParameters().getRamBufferSizeMB());
		assertEquals(9, (int)indexingParameters.getTransactionIndexParameters().getMaxMergeDocs());
		assertEquals(11, (int)indexingParameters.getTransactionIndexParameters().getMaxBufferedDocs());
		assertEquals(10, (int)indexingParameters.getTransactionIndexParameters().getMergeFactor());
		fullTextSession.close();
	}
	
	public void testBatchParameters() throws Exception {
		FullTextSession fullTextSession = Search.createFullTextSession( openSession() );
		SearchFactoryImpl searchFactory = (SearchFactoryImpl) fullTextSession.getSearchFactory();
		LuceneIndexingParameters indexingParameters = searchFactory.getIndexingParameters(searchFactory.getDirectoryProviders(Book.class)[0]);
		assertEquals(3, (int)indexingParameters.getBatchIndexParameters().getRamBufferSizeMB());
		assertEquals(12, (int)indexingParameters.getBatchIndexParameters().getMaxMergeDocs());
		assertEquals(14, (int)indexingParameters.getBatchIndexParameters().getMaxBufferedDocs());
		assertEquals(13, (int)indexingParameters.getBatchIndexParameters().getMergeFactor());
		fullTextSession.close();
	}
	
	public void testTransactionParameters() throws Exception {
		FullTextSession fullTextSession = Search.createFullTextSession( openSession() );
		SearchFactoryImpl searchFactory = (SearchFactoryImpl) fullTextSession.getSearchFactory();
		LuceneIndexingParameters indexingParameters = searchFactory.getIndexingParameters(searchFactory.getDirectoryProviders(Book.class)[0]);
		assertEquals(4, (int)indexingParameters.getTransactionIndexParameters().getRamBufferSizeMB());
		assertEquals(15, (int)indexingParameters.getTransactionIndexParameters().getMaxMergeDocs());
		assertEquals(17, (int)indexingParameters.getTransactionIndexParameters().getMaxBufferedDocs());
		assertEquals(16, (int)indexingParameters.getTransactionIndexParameters().getMergeFactor());
		fullTextSession.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
				Book.class,
				Author.class,
				Document.class
		};
	}

}
