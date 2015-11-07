/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.integration;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;

import org.hibernate.search.genericjpa.JPASearchFactoryController;
import org.hibernate.search.genericjpa.test.entities.Game;
import org.hibernate.search.genericjpa.util.Sleep;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author Martin Braun
 */
@RunWith(Arquillian.class)
public class EclipseLinkGlassFishIntegrationTest {

	private static final String[] GAME_TITLES = {
			"Super Mario Brothers",
			"Mario Kart",
			"F-Zero",
			"Wario Land",
			"Yoshi's Island",
			"Age of Empires II",
			"Warcraft III",
			"Dota 2"
	};
	@PersistenceContext
	public EntityManager em;
	@Inject
	public UserTransaction utx;
	@Inject
	private JPASearchFactoryController searchFactory;

	private static final int MAX_SLEEP_TIME = 100_000;

	@Deployment
	public static Archive<?> createDeployment() {
		return IntegrationTestUtil.createEclipseLinkMySQLDeployment();
	}

	private static boolean assertContainsAllGames(Collection<Game> retrievedGames) {
		final Set<String> retrievedGameTitles = new HashSet<>();
		for ( Game game : retrievedGames ) {
			System.out.println( "* " + game );
			retrievedGameTitles.add( game.getTitle() );
		}
		return GAME_TITLES.length == retrievedGames.size() && retrievedGameTitles.containsAll(
				Arrays.asList(
						GAME_TITLES
				)
		);
	}

	@Before
	public void setup() throws Exception {
		this.clearData();
		this.insertData();
		this.startTransaction();
	}

	@After
	public void commitTransaction() throws Exception {
		utx.commit();
	}

	private void clearData() throws Exception {
		this.utx.begin();
		this.em.joinTransaction();
		System.out.println( "Dumping old records..." );
		this.em.createQuery( "delete from Game" ).executeUpdate();
		utx.commit();

		FullTextEntityManager fem = this.searchFactory.getFullTextEntityManager( this.em );
		fem.beginSearchTransaction();
		fem.purgeAll( Game.class );
		fem.commitSearchTransaction();
	}

	private void insertData() throws Exception {
		utx.begin();
		em.joinTransaction();
		System.out.println( "Inserting records..." );
		for ( String title : GAME_TITLES ) {
			Game game = new Game( title );
			em.persist( game );
		}
		utx.commit();
		// clear the persistence context (first-level cache)
		em.clear();
	}

	@Test
	public void testMassIndexer()
			throws InterruptedException {
		Game tmp = new Game( "should not appear in index" );
		tmp.setId( -1L );
		FullTextEntityManager fem = this.searchFactory.getFullTextEntityManager( null );
		fem.beginSearchTransaction();
		fem.index( tmp );
		fem.commitSearchTransaction();
		//all the minimum stuff, so we can testCustomUpdatedEntity with our little amount of entities
		//the beefy tests are done in the jpa module anyways
		//we just want to testCustomUpdatedEntity whether we can do the indexing
		//in a EJB context :)
		this.searchFactory.pauseUpdating( true );
		try {
			this.searchFactory.getFullTextEntityManager( this.em )
					.createIndexer( Game.class )
					.batchSizeToLoadIds( 1 )
					.batchSizeToLoadObjects( 1 )
					.threadsToLoadIds( 1 )
					.threadsToLoadObjects( 1 )
							//just make sure there is no exception in setting this limit, we can't really testCustomUpdatedEntity this properly as of now
							//manual testCustomUpdatedEntity showed this was done correctly
					.idProducerTransactionTimeout( 1000 ).startAndWait();
			assertEquals(
					GAME_TITLES.length, this.searchFactory.getFullTextEntityManager( this.em ).createFullTextQuery(
							new MatchAllDocsQuery(),
							Game.class
					).getResultList().size()
			);
		}
		finally {
			this.searchFactory.pauseUpdating( false );
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldFindAllGamesInIndexBatchQuery() throws Exception {
		Sleep.sleep(
				MAX_SLEEP_TIME, () -> {
					List<Game> games = new ArrayList<>();
					FullTextEntityManager fem = this.searchFactory.getFullTextEntityManager( this.em );
					games.addAll(
							fem.createFullTextQuery( new MatchAllDocsQuery(), Game.class ).initializeObjectsWith(
									ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.QUERY
							).getResultList()
					);

					System.out.println( "Found " + games.size() + " games (using Hibernate-Search):" );
					return assertContainsAllGames( games );
				}, 100, "coudln't find all games!"
		);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldFindAllGamesInIndexFindByIdQuery() throws Exception {
		Sleep.sleep(
				MAX_SLEEP_TIME, () -> {
					List<Game> games = new ArrayList<>();
					FullTextEntityManager fem = this.searchFactory.getFullTextEntityManager( this.em );
					games.addAll(
							fem.createFullTextQuery( new MatchAllDocsQuery(), Game.class ).initializeObjectsWith(
									ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID
							).getResultList()
					);

					System.out.println( "Found " + games.size() + " games (using Hibernate-Search):" );
					return assertContainsAllGames( games );
				}, 100, "coudln't find all games!"
		);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldFindAllGamesInIndex() throws Exception {
		Sleep.sleep(
				MAX_SLEEP_TIME, () -> {
					List<Game> games = new ArrayList<>();
					FullTextEntityManager fem = this.searchFactory.getFullTextEntityManager( this.em );
					for ( String title : GAME_TITLES ) {
						FullTextQuery query = fem.createFullTextQuery(
								fem.getSearchFactory()
										.buildQueryBuilder()
										.forEntity( Game.class )
										.get()
										.keyword()
										.onField(
												"title"
										)
										.matching( title )
										.createQuery(),
								Game.class
						);
						games.addAll( query.getResultList() );
					}

					System.out.println( "Found " + games.size() + " games (using Hibernate-Search):" );
					return assertContainsAllGames( games );
				}, 100, "coudln't find all games!"
		);
	}

	@Test
	public void testManualIndexing() throws Exception {
		this.shouldFindAllGamesInIndex();
		FullTextEntityManager fem = this.searchFactory.getFullTextEntityManager( this.em );
		fem.beginSearchTransaction();
		Game newGame = new Game( "Legend of Zelda" );
		newGame.setId( -10L );
		fem.index( newGame );
		fem.commitSearchTransaction();
		Sleep.sleep(
				MAX_SLEEP_TIME, () -> {
					FullTextQuery fullTextQuery = fem.createFullTextQuery(
							fem.getSearchFactory().buildQueryBuilder().forEntity( Game.class ).get().keyword().onField(
									"title"
							).matching( "Legend of Zelda" ).createQuery(), Game.class
					);
					// we can find it in the index even though it is not persisted in the database
					boolean val1 = 1 == fullTextQuery.getResultSize();
					// but no result should be returned here:
					boolean val2 = 0 == fullTextQuery.getResultList().size();
					return val1 && val2;
				}, 100, ""
		);
	}

	@Test
	public void testRollback() throws Exception {
		this.shouldFindAllGamesInIndex();

		FullTextEntityManager fem = this.searchFactory.getFullTextEntityManager( this.em );
		fem.beginSearchTransaction();
		Game newGame = new Game( "Pong" );
		fem.index( newGame );
		newGame.setId( -10L );
		fem.rollbackSearchTransaction();
		Sleep.sleep(
				MAX_SLEEP_TIME, () -> {
					FullTextQuery fullTextQuery = fem.createFullTextQuery(
							fem.getSearchFactory()
									.buildQueryBuilder()
									.forEntity( Game.class )
									.get()
									.keyword()
									.onField(
											"title"
									)
									.matching( "Pong" )
									.createQuery(),
							Game.class
					);
					// we can find it in the index even though it is not persisted in the database
					boolean val1 = 0 == fullTextQuery.getResultSize();
					// no result should be returned here either
					boolean val2 = 0 == fullTextQuery.getResultList().size();
					return val1 && val2;
				}, 100, ""
		);
	}

	@Test
	public void testUnwrap() {
		FullTextEntityManager fem = this.searchFactory.getFullTextEntityManager( this.em );
		assertEquals( fem, fem.unwrap( FullTextEntityManager.class ) );

		FullTextQuery query = fem.createFullTextQuery( new MatchAllDocsQuery(), Game.class );
		assertEquals( query, query.unwrap( FullTextQuery.class ) );
	}

	private void startTransaction() throws Exception {
		utx.begin();
		em.joinTransaction();
	}

}
