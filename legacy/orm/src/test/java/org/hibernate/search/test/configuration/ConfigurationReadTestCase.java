/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.configuration.impl.IndexWriterSetting;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Contains some utility methods to simplify coding of test cases about configuration parsing.
 *
 * @author Sanne Grinovero
 */
public abstract class ConfigurationReadTestCase extends SearchTestBase {

	public ConfigurationReadTestCase() {
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		fullTextSession.close();
	}

	protected final void assertValueIsDefault(Class testEntity, IndexWriterSetting setting) {
		assertValueIsDefault( testEntity, 0, setting );
	}

	protected final void assertValueIsDefault(Class testEntity, int shard, IndexWriterSetting setting) {
		assertNull( "shard:" + shard + " setting:" + setting.getKey() + " : value was expected unset!",
				getParameter( shard, setting, testEntity ) );
	}

	protected final void assertValueIsSet(Class testEntity, IndexWriterSetting setting, int expectedValue) {
		assertValueIsSet( testEntity, 0, setting, expectedValue );
	}

	protected final void assertValueIsSet(Class testEntity, int shard, IndexWriterSetting setting, int expectedValue) {
		Integer actualValue = getParameter( shard, setting, testEntity );
		assertNotNull( "shard:" + shard + " setting:" + setting.getKey(), actualValue );
		assertEquals( "shard:" + shard + " setting:" + setting.getKey(), expectedValue, (int) actualValue );
	}

	private Integer getParameter(int shard, IndexWriterSetting setting, Class testEntity) {
		EntityIndexBinding mappingForEntity = getExtendedSearchIntegrator().getIndexBindings().get( testEntity );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) getIndexManager( mappingForEntity, shard );
		LuceneIndexingParameters luceneIndexingParameters = indexManager.getIndexingParameters();
		return luceneIndexingParameters.getIndexParameters().getCurrentValueFor( setting );
	}

	protected final IndexManager getIndexManager(EntityIndexBinding mappingForEntity, int shardIndex) {
		Set<IndexManager> indexManagers = mappingForEntity.getIndexManagerSelector().all();
		if ( shardIndex == 0 && indexManagers.size() == 1 ) {
			return indexManagers.iterator().next();
		}
		else {
			for ( IndexManager indexManager : mappingForEntity.getIndexManagerSelector().all() ) {
				if ( indexManager.getIndexName().endsWith( "." + shardIndex ) ) {
					return indexManager;
				}
			}
			throw new IllegalStateException( "Could not find IndexManager for shard " + shardIndex
					+ " for binding " + mappingForEntity );
		}
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.indexBase", getBaseIndexDir().toAbsolutePath().toString() );
	}

	public static void assertCfgIsInvalid(Configuration configuration, Class[] mapping) {
		try {
			for ( Class annotated : mapping ) {
				( configuration ).addAnnotatedClass( annotated );
			}
			configuration.setProperty( "hibernate.search.default.directory_provider", "local-heap" );
			configuration.buildSessionFactory();
			fail();
		}
		catch (HibernateException e) {
			//thrown exceptions means the test is ok when caused by a SearchException
			Throwable cause = e.getCause();
			assertTrue( cause instanceof SearchException );
		}
	}

}
