/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.db.events.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.genericjpa.db.EventType;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer;
import org.hibernate.search.genericjpa.db.events.impl.AnnotationEventModelParser;
import org.hibernate.search.genericjpa.db.events.impl.EventModelInfo;
import org.hibernate.search.genericjpa.db.events.impl.EventModelParser;
import org.hibernate.search.genericjpa.db.events.jpa.impl.JPAUpdateSource;
import org.hibernate.search.genericjpa.db.events.triggers.HSQLDBTriggerSQLStringSource;
import org.hibernate.search.genericjpa.jpa.util.impl.JPAEntityManagerFactoryWrapper;
import org.hibernate.search.genericjpa.jpa.util.impl.JPAEntityManagerWrapper;
import org.hibernate.search.genericjpa.jpa.util.impl.MultiQueryAccess;
import org.hibernate.search.genericjpa.test.jpa.entities.Place;
import org.hibernate.search.genericjpa.test.jpa.entities.Sorcerer;
import org.hibernate.search.genericjpa.util.Sleep;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Martin Braun
 */
public class JPAUpdateSourceTest {

	/**
	 * this is needed in other tests because the query method of JPAUpdateSource has package access
	 */
	public static MultiQueryAccess query(EntityManagerFactory emf, EntityManager em) throws NoSuchFieldException {
		EventModelParser parser = new AnnotationEventModelParser();
		JPAUpdateSource updateSource = new JPAUpdateSource(
				parser.parse( new HashSet<>( Arrays.asList( Place.class, Sorcerer.class ) ) ),
				new JPAEntityManagerFactoryWrapper( emf, null ), 1, TimeUnit.SECONDS, 2, 2, "\""
		);
		return JPAUpdateSource.query( updateSource, new JPAEntityManagerWrapper( em, null ) );
	}

	@Test
	public void test() throws InterruptedException {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "EclipseLink_HSQLDB" );
		try {
			EventModelParser parser = new AnnotationEventModelParser();
			HSQLDBTriggerSQLStringSource triggerSource = new HSQLDBTriggerSQLStringSource();
			{
				EntityManager em = null;
				JPAUpdateSource updateSource;
				try {
					em = emf.createEntityManager();
					List<EventModelInfo> infos = parser.parse(
							new HashSet<>(
									Arrays.asList(
											Place.class,
											Sorcerer.class
									)
							)
					);
					em.getTransaction().begin();
					for ( EventModelInfo info : infos ) {
						for ( String str : triggerSource.getUpdateTableDropCode( info ) ) {
							System.out.println( str );
							em.createNativeQuery( str ).executeUpdate();
						}
						for ( String str : triggerSource.getUpdateTableCreationCode( info ) ) {
							System.out.println( str );
							em.createNativeQuery( str ).executeUpdate();
						}
					}
					em.getTransaction().commit();
					updateSource = new JPAUpdateSource(
							infos,
							new JPAEntityManagerFactoryWrapper( emf, null ),
							1,
							TimeUnit.SECONDS,
							2,
							2,
							triggerSource.getDelimitedIdentifierToken()
					);

					EntityTransaction tx = em.getTransaction();

					tx.begin();
					em.createNativeQuery(
							String.format(
									(Locale) null,
									"DELETE FROM \"PlaceSorcererUpdatesHsearch\"",
									String.valueOf( EventType.INSERT )
							)
					).executeUpdate();
					tx.commit();

					tx.begin();
					em.createNativeQuery(
							String.format(
									(Locale) null,
									"INSERT INTO \"PlaceSorcererUpdatesHsearch\"(\"updateid\", \"eventCase\", \"placefk\", \"sorcererfk\") VALUES (1, %s, 2, 3)",
									String.valueOf( EventType.INSERT )
							)
					).executeUpdate();
					tx.commit();

				}
				finally {
					if ( em != null ) {
						em.close();
					}
				}
				final boolean[] gotEvent = new boolean[2];
				updateSource.setUpdateConsumers(
						Arrays.asList(
								new UpdateConsumer() {

									@Override
									public void updateEvent(List<UpdateEventInfo> updateInfos) {
										for ( UpdateEventInfo updateInfo : updateInfos ) {
											Object id = updateInfo.getId();
											int eventType = updateInfo.getEventType();
											if ( id.equals( 2 ) && Place.class.equals( updateInfo.getEntityClass() ) && EventType.INSERT == eventType ) {
												gotEvent[0] = true;
											}
											else if ( id.equals( 3 ) && updateInfo.getEntityClass()
													.equals( Sorcerer.class ) && eventType == EventType.INSERT ) {
												gotEvent[1] = true;
											}
										}
									}
								}
						)
				);
				updateSource.start();
				Sleep.sleep(
						1000 * 100, () -> {
							for ( boolean ev : gotEvent ) {
								if ( !ev ) {
									return false;
								}
							}
							return true;
						}, 100, ""
				);
				updateSource.stop();
			}


			EntityManager em = null;
			try {
				em = emf.createEntityManager();
				EntityTransaction tx = em.getTransaction();
				tx.begin();
				assertEquals(
						"AsyncUpdateSource should delete all things after it has processed the updates but didn't do so",
						0,
						em.createNativeQuery( "SELECT * FROM \"PlaceSorcererUpdatesHsearch\"" ).getResultList().size()
				);
				tx.commit();
			}
			finally {
				if ( em != null ) {
					em.close();
				}
			}

		}

		finally

		{
			emf.close();
		}
	}

}
