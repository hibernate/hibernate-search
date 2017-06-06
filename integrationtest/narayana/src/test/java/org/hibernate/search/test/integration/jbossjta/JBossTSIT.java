/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jbossjta;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.lucene.search.Query;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.integration.jbossjta.infra.JBossTADataSourceBuilder;
import org.hibernate.search.test.integration.jbossjta.infra.PersistenceUnitInfoBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;

/**
 * @author Emmanuel Bernard
 */

public class JBossTSIT {

	private static EntityManagerFactory factory;

	@BeforeClass
	public static void setUp() throws Exception {

		//DataSource configuration
		final String url = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1";
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
				.setPersistenceProviderClassName( HibernatePersistenceProvider.class.getName() )
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
				.addProperty( "hibernate.search.default.directory_provider", "local-heap" )
				.create();
		final HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		factory = hp.createContainerEntityManagerFactory( unitInfo, new HashMap() );

	}

	@AfterClass
	public static void tearDown() throws Exception {
		if ( factory != null ) {
			factory.close();
		}
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
