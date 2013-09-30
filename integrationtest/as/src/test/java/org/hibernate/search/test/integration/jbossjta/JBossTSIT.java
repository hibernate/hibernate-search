/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.integration.jbossjta;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;

import org.apache.lucene.search.Query;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.integration.jbossjta.infra.JBossTADataSourceBuilder;
import org.hibernate.search.test.integration.jbossjta.infra.PersistenceUnitInfoBuilder;
import org.hibernate.search.util.impl.FileHelper;

/**
 * @author Emmanuel Bernard
 */

public class JBossTSIT {

	private static EntityManagerFactory factory;
	public static File tempDirectory = new File( TestConstants.getTargetDir( JBossTSIT.class ) + "/h2" );

	@BeforeClass
	public static void setUp() throws Exception {
		FileHelper.delete( tempDirectory );
		tempDirectory.mkdir();

		//DataSource configuration
		final String url = "jdbc:h2:file:" + tempDirectory.getAbsolutePath() + "/h2db";
		final String user = "sa";
		final String password = "";

		//H2 DataSource creation
		final JdbcDataSource underlyingDataSource = new JdbcDataSource();
		underlyingDataSource.setURL( url );
		underlyingDataSource.setUser( user );
		underlyingDataSource.setPassword( password );

		//build JBoss-bound DataSource
		DataSource ds = new JBossTADataSourceBuilder()
				.setXADataSource( underlyingDataSource )
				.setUser( user )
				.setPassword( password )
				.setTimeout( 0 ) //infinite transaction
				.createDataSource();

		PersistenceUnitInfoBuilder pub = new PersistenceUnitInfoBuilder();
		final PersistenceUnitInfo unitInfo = pub
				.setExcludeUnlistedClasses( true )
				.setJtaDataSource( ds )
				.setPersistenceProviderClassName( HibernatePersistence.class.getName() )
				.setPersistenceUnitName( "jbossjta" )
				.setPersistenceXMLSchemaVersion( "2.0" )
				.setSharedCacheMode( SharedCacheMode.NONE )
				.setValidationMode( ValidationMode.NONE )
				.setTransactionType( PersistenceUnitTransactionType.JTA )
				.addManagedClassNames( Tweet.class.getName() )
				.addProperty( "hibernate.dialect", H2Dialect.class.getName() )
				.addProperty( Environment.HBM2DDL_AUTO, "create-drop" )
				.addProperty( Environment.SHOW_SQL, "true" )
				.addProperty( Environment.JTA_PLATFORM, JBossStandAloneJtaPlatform.class.getName() )
						//I don't pool connections by JTA transaction. Leave the work to Hibernate Core
				.addProperty( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_TRANSACTION.toString() )
				.addProperty( "hibernate.search.default.directory_provider", "ram" )
				.create();
		final HibernatePersistence hp = new HibernatePersistence();
		factory = hp.createContainerEntityManagerFactory( unitInfo, new HashMap() );

	}

	@AfterClass
	public static void tearDown() {
		if ( factory != null ) {
			factory.close();
		}
		FileHelper.delete( tempDirectory );
	}

	@Test
	public void testJBossTS() throws Exception {
		TransactionManagerImple tm = new TransactionManagerImple();
		tm.begin();
		EntityManager em = factory.createEntityManager();
		Tweet tweet = new Tweet( "Spice is the essence of life" );
		em.persist( tweet );
		tm.commit();
		em.close();

		tm.begin();
		em = factory.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		final QueryBuilder builder = ftem.getSearchFactory().buildQueryBuilder().forEntity( Tweet.class ).get();
		final Query query = builder
				.keyword()
				.onField( "text" )
				.matching( "spice" )
				.createQuery();

		ftem.createFullTextQuery( query, Tweet.class ).getResultList();
		final List resultList = em.createQuery( "from " + Tweet.class.getName() ).getResultList();
		Assert.assertEquals( 1, resultList.size() );
		for ( Object o : resultList ) {
			em.remove( o );
		}
		tm.commit();

		em.close();
	}
}
