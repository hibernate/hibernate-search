/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.db.events.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.genericjpa.db.events.impl.AnnotationEventModelParser;
import org.hibernate.search.genericjpa.db.events.impl.EventModelInfo;
import org.hibernate.search.genericjpa.db.events.impl.EventModelParser;
import org.hibernate.search.genericjpa.db.events.index.impl.IndexUpdater;
import org.hibernate.search.genericjpa.db.events.jpa.impl.JPAUpdateSource;
import org.hibernate.search.genericjpa.db.events.triggers.MySQLTriggerSQLStringSource;
import org.hibernate.search.genericjpa.entity.ReusableEntityProvider;
import org.hibernate.search.genericjpa.entity.impl.JPAReusableEntityProvider;
import org.hibernate.search.genericjpa.factory.StandaloneSearchConfiguration;
import org.hibernate.search.genericjpa.jpa.util.impl.JPAEntityManagerFactoryWrapper;
import org.hibernate.search.genericjpa.metadata.impl.MetadataRehasher;
import org.hibernate.search.genericjpa.metadata.impl.MetadataUtil;
import org.hibernate.search.genericjpa.metadata.impl.RehashedTypeMetadata;
import org.hibernate.search.genericjpa.test.jpa.entities.Place;
import org.hibernate.search.genericjpa.test.jpa.entities.Sorcerer;
import org.hibernate.search.genericjpa.util.Sleep;
import org.hibernate.search.spi.SearchIntegratorBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Martin Braun
 */
public class ManualUpdateIntegrationTest extends DatabaseIntegrationTest {

	Map<Class<?>, List<Class<?>>> containedInIndexOf;
	Map<Class<?>, RehashedTypeMetadata> rehashedTypeMetadataPerIndexRoot;
	ReusableEntityProvider entityProvider;
	MetaModelParser metaModelParser;

	@Before
	public void setup() throws SQLException {
		MetadataProvider metadataProvider = MetadataUtil.getDummyMetadataProvider( new StandaloneSearchConfiguration() );
		MetadataRehasher rehasher = new MetadataRehasher();
		List<RehashedTypeMetadata> rehashedTypeMetadatas = new ArrayList<>();
		rehashedTypeMetadataPerIndexRoot = new HashMap<>();
		for ( Class<?> indexRootType : Arrays.asList( Place.class ) ) {
			RehashedTypeMetadata rehashed = rehasher.rehash( metadataProvider.getTypeMetadataFor( indexRootType ) );
			rehashedTypeMetadatas.add( rehashed );
			rehashedTypeMetadataPerIndexRoot.put( indexRootType, rehashed );
		}
		this.containedInIndexOf = MetadataUtil.calculateInIndexOf( rehashedTypeMetadatas );
		this.setup( "EclipseLink_MySQL", new MySQLTriggerSQLStringSource() );
		this.metaModelParser = new MetaModelParser();
		this.metaModelParser.parse( this.emf.getMetamodel() );
	}

	@Test
	public void test() throws SQLException, InterruptedException {
		this.setupTriggers( new MySQLTriggerSQLStringSource() );
		try {
			if ( this.exceptionString != null ) {
				fail( exceptionString );
			}
			SearchConfiguration searchConfiguration = new StandaloneSearchConfiguration();

			SearchIntegratorBuilder builder = new SearchIntegratorBuilder();
			// we have to build an integrator here (but we don't need it
			// afterwards)
			builder.configuration( searchConfiguration ).buildSearchIntegrator();
			metaModelParser.getIndexRelevantEntites().forEach(
					(clazz) -> {
						builder.addClass( clazz );
					}
			);
			ExtendedSearchIntegrator impl = (ExtendedSearchIntegrator) builder.buildSearchIntegrator();
			JPAReusableEntityProvider entityProvider = new JPAReusableEntityProvider(
					this.emf,
					this.metaModelParser.getIdProperties()
			);
			IndexUpdater indexUpdater = new IndexUpdater(
					this.rehashedTypeMetadataPerIndexRoot,
					this.containedInIndexOf,
					entityProvider,
					impl
			);
			try {
				EventModelParser eventModelParser = new AnnotationEventModelParser();
				List<EventModelInfo> eventModelInfos = eventModelParser.parse(
						new HashSet<>(
								Arrays.asList(
										Place.class, Sorcerer.class
								)
						)
				);
				JPAUpdateSource updateSource = new JPAUpdateSource(
						eventModelInfos,
						new JPAEntityManagerFactoryWrapper( this.emf, null ),
						100,
						TimeUnit.MILLISECONDS,
						10, new MySQLTriggerSQLStringSource().getDelimitedIdentifierToken()
				);
				updateSource.setUpdateConsumers( Arrays.asList( indexUpdater::updateEvent ) );
				updateSource.start();

				try {

					// database already contains stuff, so clear everything out here
					EntityManager em = this.emf.createEntityManager();
					try {
						if ( !this.assertCount( impl, 0 ) ) {
							throw new AssertionError();
						}
						this.deleteAllData( em );
						Sleep.sleep(
								100_000, () ->
										this.assertCount( impl, 0 )
								, 100, ""
						);

						this.writeAllIntoIndex( em, impl );

						this.deleteAllData( em );
						Sleep.sleep(
								100_000, () -> this.assertCount( impl, 0 ), 100, ""
						);

						this.writeAllIntoIndex( em, impl );

						{
							List<Integer> places = this.queryPlaceIds( impl, "name", "Valinor" );
							assertEquals(
									"this testCustomUpdatedEntity expects to have exactly one Place named Valinor!",
									1,
									places.size()
							);
							Integer valinorId = places.get( 0 );

							{
								EntityTransaction tx = em.getTransaction();
								tx.begin();
								Place valinor = em.find( Place.class, valinorId );
								valinor.setName( "Alinor" );
								em.persist( valinor );
								tx.commit();
							}
							Sleep.sleep(
									100_000,
									() -> this.queryPlaceIds( impl, "name", "Valinor" )
											.size() == 0 && this.assertCount( impl, 2 )
									,
									100,
									"shouldn't have found \"Valinor\" in the index anymore, but overall count should have been equal to 2!"
							);
							{
								String oldName;
								{
									EntityTransaction tx = em.getTransaction();
									tx.begin();
									Place valinor = em.find( Place.class, valinorId );
									Sorcerer someSorcerer = valinor.getSorcerers().iterator().next();
									oldName = someSorcerer.getName();
									assertEquals(
											"should have found \"" + oldName + "\" in the index!",
											1,
											this.queryPlaceIds(
													impl,
													"sorcerers.name",
													oldName
											).size()
									);
									someSorcerer.setName( "Odalbert" );
									tx.commit();
								}
								Sleep.sleep(
										100_000,
										() ->
												this.queryPlaceIds( impl, "sorcerers.name", oldName )
														.size() == 0 && this
														.assertCount( impl, 2 )
										, 100,
										"shouldn't have found \"" + oldName + "\" in the index anymore, but overall count should have been equal to 2!"
								);
							}
						}
						this.deleteAllData( em );
					}
					finally {
						if ( em != null ) {
							em.close();
						}
					}
				}
				finally {
					updateSource.stop();
				}
			}
			finally {
				indexUpdater.close();
			}
		}
		finally {
			this.tearDownTriggers();
		}
	}

	private void writeAllIntoIndex(EntityManager em, ExtendedSearchIntegrator impl) throws InterruptedException {
		// and write data in the index again
		this.setupData( em );
		// wait a bit until the AsyncUpdateSource sent the appropriate events
		Sleep.sleep(
				100_000, () -> this.assertCount( impl, 2 ), 100, ""
		);
	}

	private boolean assertCount(ExtendedSearchIntegrator impl, int count) {
		return count == impl.createHSQuery()
				.targetedEntities( Arrays.asList( Place.class ) )
				.luceneQuery( impl.buildQueryBuilder().forEntity( Place.class ).get().all().createQuery() )
				.queryResultSize();
	}

	private List<Integer> queryPlaceIds(ExtendedSearchIntegrator impl, String field, String value) {
		return impl.createHSQuery().targetedEntities( Arrays.asList( Place.class ) )
				.luceneQuery(
						impl.buildQueryBuilder()
								.forEntity( Place.class )
								.get()
								.keyword()
								.onField( field )
								.matching( value )
								.createQuery()
				)
				.queryEntityInfos().stream().map(
						(entInfo) -> (Integer) entInfo.getId()
				).collect( Collectors.toList() );
	}

}
