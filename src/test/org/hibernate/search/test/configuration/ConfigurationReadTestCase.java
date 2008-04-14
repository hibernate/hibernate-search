package org.hibernate.search.test.configuration;

import java.io.File;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.configuration.IndexWriterSetting;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.impl.SearchFactoryImpl;
import org.hibernate.search.test.SearchTestCase;

/**
 * Contains some utility methods to simplify coding of
 * testcases about configuration parsing.
 *
 * @author Sanne Grinovero
 */
public abstract class ConfigurationReadTestCase extends SearchTestCase {
	
	private static final File INDEX_DIR = new File( new File("."), "indextemp" );

	private SearchFactoryImplementor searchFactory;

	protected enum TransactionType {
		TRANSACTION, BATCH
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		FullTextSession fullTextSession = Search.createFullTextSession( openSession() );
		searchFactory = (SearchFactoryImpl) fullTextSession.getSearchFactory();
		fullTextSession.close();
		remove(INDEX_DIR);
		INDEX_DIR.mkdirs();
	}

	protected final void assertValueIsDefault(Class testEntity, TransactionType parmGroup, IndexWriterSetting setting) {
		assertValueIsDefault( testEntity, 0, parmGroup, setting );
	}

	protected final void assertValueIsDefault(Class testEntity, int shard, TransactionType parmGroup, IndexWriterSetting setting) {
		boolean batch = isBatch( parmGroup );
		assertNull( "shard:" + shard + " batch=" + batch + " setting:" + setting.getKey() + " : value was expected unset!",
				getParameter( shard, batch, setting, testEntity ) );
	}

	protected final void assertValueIsSet(Class testEntity, TransactionType parmGroup, IndexWriterSetting setting, int expectedValue) {
		assertValueIsSet( testEntity, 0, parmGroup, setting, expectedValue );
	}

	protected final void assertValueIsSet(Class testEntity, int shard, TransactionType parmGroup, IndexWriterSetting setting, int expectedValue) {
		boolean batch = isBatch( parmGroup );
		assertNotNull( "shard:" + shard + " batch=" + batch + " setting:" + setting.getKey(),
				getParameter( shard, batch, setting, testEntity ) );
		assertEquals( "shard:" + shard + " batch=" + batch + " setting:" + setting.getKey(), expectedValue,
				(int) getParameter( shard, batch, setting, testEntity ) );
	}

	protected final SearchFactoryImplementor getSearchFactory() {
		return searchFactory;
	}

	private boolean isBatch(TransactionType parmGroup) {
		return parmGroup == TransactionType.BATCH;
	}

	private Integer getParameter(int shard, boolean batch, IndexWriterSetting setting, Class testEntity) {
		if ( batch ) {
			return searchFactory.getIndexingParameters( searchFactory.getDirectoryProviders( testEntity )[shard] )
															.getBatchIndexParameters().getCurrentValueFor( setting );
		}
		else {
			return searchFactory.getIndexingParameters( searchFactory.getDirectoryProviders( testEntity )[shard] )
															.getTransactionIndexParameters().getCurrentValueFor( setting );
		}
	}
	
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.indexBase", INDEX_DIR.getAbsolutePath() );
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		remove(INDEX_DIR);
	}
	
	private void remove(File indexDir) {
		if (indexDir.exists()) {
			File[] containing = indexDir.listFiles();
			if ( containing != null ) { //is a directory
				for (int i=0; i<containing.length; i++ ){
					remove(containing[i]);
				}
			}
			indexDir.delete();
		}
	}

}
