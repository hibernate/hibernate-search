/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.optimizations;

import java.lang.annotation.ElementType;

import org.hibernate.Transaction;

import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.cfg.EntityMapping;
import org.hibernate.search.cfg.IndexedMapping;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verify that updates to collections that are not indexed do not trigger indexing.
 * Updating a collection in an entity which is not indexed, but has some other property marked
 * as containedIn, should generally not trigger indexing of the containedIn objects, unless
 * we can't tell for sure the index state is not going to be affected.
 *
 * @author Sanne Grinovero
 * @author Tom Waterhouse
 */
@TestForIssue(jiraKey = "HSEARCH-679")
public class CollectionUpdateEventTest {

	private static boolean WITH_CLASS_BRIDGE_ON_ITEM = true;
	private static boolean WITHOUT_CLASS_BRIDGE_ON_ITEM = false;

	private static boolean WITH_CLASS_BRIDGE_ON_CATALOG = true;
	private static boolean WITHOUT_CLASS_BRIDGE_ON_CATALOG = false;

	/**
	 * If the top level class has a class bridge or dynamic boost, then we can't safely
	 * enable this optimization.
	 */
	@Test
	public void testWithClassBridge() {
		testScenario( WITH_CLASS_BRIDGE_ON_ITEM, 2, WITHOUT_CLASS_BRIDGE_ON_CATALOG );
	}

	/**
	 * The indexing should be skipped when no custom bridges are used.
	 */
	@Test
	public void testWithoutClassBridge() {
		testScenario( WITHOUT_CLASS_BRIDGE_ON_ITEM, 2, WITHOUT_CLASS_BRIDGE_ON_CATALOG );
	}

	/**
	 * Test having a depth which is not enough to reach the dirty collection.
	 * We should be able to optimize this case.
	 */
	@Test
	public void testWithNoEnoughDepth() {
		testScenario( WITH_CLASS_BRIDGE_ON_ITEM, 1, WITHOUT_CLASS_BRIDGE_ON_CATALOG );
	}

	/**
	 * Test again with a no-enough-depth scenario, but having the class bridge
	 * defined close to the collection (far from the root entity)
	 */
	@Test
	public void testWithDeepClassBridge() {
		testScenario( WITHOUT_CLASS_BRIDGE_ON_ITEM, 1, WITH_CLASS_BRIDGE_ON_CATALOG );
	}

	private void testScenario(boolean withClassBridgeOnItem, int depth, boolean withClassBridgeOnCatalog) {
		FullTextSessionBuilder fulltextSessionBuilder = configure(
				withClassBridgeOnItem,
				depth,
				withClassBridgeOnCatalog
		);
		try {
			initializeData( fulltextSessionBuilder );
			FullTextSession fullTextSession = fulltextSessionBuilder.openFullTextSession();
			try {
				Catalog catalog = (Catalog) fullTextSession.get( Catalog.class, 1L );
				PersistentSet catalogItems = (PersistentSet) catalog.getCatalogItems();
				PersistentBag consumers = (PersistentBag) catalog.getConsumers();

				assertFalse( "consumers should not be initialized", consumers.wasInitialized() );
				assertFalse( "catalogItems should not be initialized", consumers.wasInitialized() );

				updateCatalogsCollection( fullTextSession, catalog );

				if ( ( withClassBridgeOnItem || withClassBridgeOnCatalog ) && depth > 1 ) {
					assertTrue( "catalogItems should have been initialized", catalogItems.wasInitialized() );
				}
				else {
					assertFalse( "catalogItems should not be initialized", catalogItems.wasInitialized() );
				}
			}
			finally {
				fullTextSession.close();
			}
		}
		finally {
			fulltextSessionBuilder.close();
		}
	}

	private FullTextSessionBuilder configure(boolean withClassBridgeOnItem, int depth, boolean withClassBridgeOnCatalog) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
				.addAnnotatedClass( Catalog.class )
				.addAnnotatedClass( CatalogItem.class )
				.addAnnotatedClass( Consumer.class )
				.addAnnotatedClass( Item.class );
		SearchMapping fluentMapping = builder.fluentMapping();
		// mapping for Catalog
		EntityMapping catalogMapping = fluentMapping
				.entity( Catalog.class );
		if ( withClassBridgeOnCatalog ) {
			catalogMapping.classBridge( NoopClassBridge.class );
		}
		catalogMapping
				.property( "catalogItems", ElementType.FIELD ).containedIn();

		// mapping for CatalogItem
		fluentMapping.entity( CatalogItem.class )
				.property( "item", ElementType.FIELD ).containedIn()
				.property( "catalog", ElementType.FIELD ).indexEmbedded();

		// mapping for Item
		IndexedMapping itemMapping = fluentMapping
				.entity( Item.class )
				.indexed();
		if ( withClassBridgeOnItem ) {
			itemMapping.classBridge( NoopClassBridge.class );
		}
		itemMapping.property( "catalogItems", ElementType.FIELD ).indexEmbedded().depth( depth );
		return builder.build();
	}

	private void initializeData(FullTextSessionBuilder fulltextSessionBuilder) {
		FullTextSession fullTextSession = fulltextSessionBuilder.openFullTextSession();
		try {
			final Transaction transaction = fullTextSession.beginTransaction();

			Catalog catalog = new Catalog();
			catalog.setCatalogId( 1L );
			catalog.setName( "parts" );
			fullTextSession.persist( catalog );

			for ( int i = 0; i < 5; i++ ) {
				Item item = new Item();
				item.setName( "battery" );
				fullTextSession.persist( item );

				CatalogItem catalogItem = new CatalogItem();
				catalogItem.setCatalog( catalog );
				catalogItem.setItem( item );
				fullTextSession.persist( catalogItem );

				item.getCatalogItems().add( catalogItem );
				fullTextSession.merge( item );

				catalog.getCatalogItems().add( catalogItem );
				fullTextSession.merge( catalog );
			}

			transaction.commit();
		}
		finally {
			fullTextSession.close();
		}
	}

	/**
	 * Update a non-indexed collection of an entity contained in a collection. No indexing work should be created.
	 */
	private void updateCatalogsCollection(FullTextSession fullTextSession, Catalog catalog) {
		final Transaction transaction = fullTextSession.beginTransaction();

		Consumer consumer = new Consumer();
		consumer.setName( "consumer" );
		consumer.getCatalogs().add( catalog );
		fullTextSession.persist( consumer );

		catalog.getConsumers().add( consumer );
		fullTextSession.merge( catalog );

		transaction.commit();
	}

}
