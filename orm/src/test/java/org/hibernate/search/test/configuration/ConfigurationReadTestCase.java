/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.configuration;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.configuration.impl.IndexWriterSetting;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.test.SearchTestCase;

/**
 * Contains some utility methods to simplify coding of test cases about configuration parsing.
 *
 * @author Sanne Grinovero
 */
public abstract class ConfigurationReadTestCase extends SearchTestCase {

	private SearchFactoryImplementor searchFactory;

	public ConfigurationReadTestCase() {
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();
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
		assertNotNull( "shard:" + shard + " setting:" + setting.getKey(),
				getParameter( shard, setting, testEntity ) );
		assertEquals( "shard:" + shard + " setting:" + setting.getKey(), expectedValue,
				(int) getParameter( shard, setting, testEntity ) );
	}

	@Override
	public final SearchFactoryImplementor getSearchFactory() {
		return searchFactory;
	}

	private Integer getParameter(int shard, IndexWriterSetting setting, Class testEntity) {
		EntityIndexBinding mappingForEntity = searchFactory.getIndexBinding( testEntity );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) mappingForEntity.getIndexManagers()[shard];
		LuceneIndexingParameters luceneIndexingParameters = indexManager.getIndexingParameters();
		return luceneIndexingParameters.getIndexParameters().getCurrentValueFor( setting );
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() );
	}

	public static void assertCfgIsInvalid(Configuration configuration, Class[] mapping) {
		try {
			for ( Class annotated : mapping ) {
				( configuration ).addAnnotatedClass( annotated );
			}
			configuration.setProperty( "hibernate.search.default.directory_provider", "ram" );
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
