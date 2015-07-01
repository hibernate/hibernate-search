/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.massindexing;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.inject.Inject;

import org.hibernate.search.test.integration.VersionTestHelper;
import org.hibernate.search.test.integration.wildfly.PackagerHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Reproducer for timeouts during mass indexing.
 *
 * @author Gunnar Morling
 */
@RunWith(Arquillian.class)
@TestForIssue(jiraKey = "HSEARCH-1783")
public class MassIndexingTimeoutIT {

	private static final int NUMBER_OF_ENTIIES = 2000;

	@Deployment
	public static Archive<?> createTestArchive() {
		WebArchive archive = ShrinkWrap
				.create( WebArchive.class, MassIndexingTimeoutIT.class.getSimpleName() + ".war" )
				.addClasses( Concert.class, ConcertManager.class )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsLibraries( PackagerHelper.hibernateSearchLibraries() )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		return archive;
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.clazz( Concert.class.getName() )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.getOrCreateProperties()
					//Only needed because we override the Hibernate ORM module with a custom one:
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( VersionTestHelper.injectVariables( "org.hibernate.search.hibernate-orm-repackage:${dependency.version.HibernateSearch}" ) ).up()
					//Only needed because the above custom module also disables the default integration adapters:
					.createProperty().name( "hibernate.transaction.jta.platform" ).value( "org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform" ).up()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
					.createProperty().name( "hibernate.search.indexing_strategy" ).value( "manual" ).up()
					.createProperty().name( "hibernate.jdbc.batch_size" ).value( "50" ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Inject
	private ConcertManager concertManager;

	/**
	 * Triggers a timeout during id production. The TX timeout, batch size and test data is chosen to ensure that id
	 * consumption applies back-pressure on the {@code ProducerConsumerQueue}, causing the id producer to run into a TX
	 * timeout. More specifically, consumption of the ids takes that long (by means of artificial slow-down in
	 * {@link Concert}, that the id producer cannot emit all 2000 ids before its transaction timesout.
	 * <p>
	 * The timeout is given via {@code coordinator-environment#default-timeout} in standalone-full-testqueues.xml.
	 */
	@Test
	public void timeoutDuringIdProduction() throws Exception {
		insertTestData();

		assertEquals( 0, concertManager.findConcertsByArtist( "Hruce Bronsby" ).size() );

		Concert.SLOW_DOWN = true;
		concertManager.indexConcerts();
		Concert.SLOW_DOWN = false;

		List<Concert> artists = concertManager.findConcertsByArtist( "Hruce Bronsby" );

		assertTrue( "Expecting more than 1000 entries to be indexed, as the first 1000 items can be added to the"
				+ "ProducerConsumerQueue without any delay; Actual: " + artists.size(), artists.size() > 1000 );

		assertTrue( "Expecting not all entries to be indexed, as the transaction timeout is too short to produce all"
				+ "items; Actual: " + artists.size(), artists.size() < NUMBER_OF_ENTIIES );
	}

	private void insertTestData() {
		List<Concert> concerts = new ArrayList<>();

		for ( int i = 0; i < NUMBER_OF_ENTIIES; i++ ) {
			concerts.add( new Concert( "Hruce Bronsby", new GregorianCalendar( 2015, 0, 31 ).getTime() ) );
		}

		concertManager.saveConcerts( concerts );
	}
}
