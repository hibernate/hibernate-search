/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.search.test.engine.optimizations;

import java.lang.annotation.ElementType;

import org.hibernate.Transaction;

import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.cfg.EntityMapping;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.test.util.TestForIssue;
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
@TestForIssue( jiraKey = "HSEARCH-679")
public class CollectionUpdateEventTest {

	/**
	 * If the top level class has a class bridge or dynamic boost, then we can't safely
	 * enable this optimization.
	 */
	@Test
	public void testWithClassBridge() {
		testScenario( true, 2, false );
	}

	/**
	 * The indexing should be skipped when no custom bridges are used.
	 */
	@Test
	public void testWithoutClassBridge() {
		testScenario( false, 2, false );
	}

	/**
	 * Test having a depth which is not enough to reach the dirty collection.
	 * We should be able to optimize this case.
	 */
	@Test
	public void testWithNoEnoughDepth() {
		testScenario( true, 1, false );
	}

	/**
	 * Test again with a no-enough-depth scenario, but having the class bridge
	 * defined close to the collection (far from the root entity)
	 */
	@Test
	public void testWithDeepClassBridge() {
		testScenario( false, 1, true );
	}

	private void testScenario(boolean usingClassBridge, int depth, boolean usingClassBridgeOnEmbedded) {
		FullTextSessionBuilder fulltextSessionBuilder = createSearchFactory(
				usingClassBridge,
				depth,
				usingClassBridgeOnEmbedded
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

				if ( ( usingClassBridge || usingClassBridgeOnEmbedded ) && depth > 1 ) {
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

	private FullTextSessionBuilder createSearchFactory(boolean defineClassBridge, int depth, boolean usingClassBridgeOnEmbedded) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
				.addAnnotatedClass( Catalog.class )
				.addAnnotatedClass( CatalogItem.class )
				.addAnnotatedClass( Consumer.class )
				.addAnnotatedClass( Item.class );
		SearchMapping fluentMapping = builder.fluentMapping();
		EntityMapping catalogMapping = fluentMapping
				.entity( Catalog.class );
		if ( usingClassBridgeOnEmbedded ) {
			catalogMapping.classBridge( ItemClassBridge.class );
		}
		catalogMapping
				.property( "catalogItems", ElementType.FIELD ).containedIn()
				.entity( CatalogItem.class )
				.property( "item", ElementType.FIELD ).containedIn()
				.property( "catalog", ElementType.FIELD ).indexEmbedded();
		if ( defineClassBridge ) {
			fluentMapping
					.entity( Item.class )
					.classBridge( ItemClassBridge.class )
					.indexed()
					.property( "catalogItems", ElementType.FIELD ).indexEmbedded().depth( depth );
		}
		else {
			fluentMapping
					.entity( Item.class )
					.indexed()
					.property( "catalogItems", ElementType.FIELD ).indexEmbedded().depth( depth );
		}
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
