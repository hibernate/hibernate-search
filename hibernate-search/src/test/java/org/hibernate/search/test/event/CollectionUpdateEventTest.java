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

package org.hibernate.search.test.event;

import java.lang.annotation.ElementType;

import org.hibernate.Transaction;
import org.hibernate.collection.PersistentBag;
import org.hibernate.collection.PersistentSet;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

/**
 * HSEARCH-679 - verify that updates to collections that are not indexed do not trigger indexing.
 * 
 * @author Sanne Grinovero
 * @author Tom Waterhouse
 */
public class CollectionUpdateEventTest {

	@Test
	public void testWithClassBridge() {
		testScenario( true );
	}
	
	@Test
	public void testWithoutClassBridge() {
		testScenario( false );
	}
	
	private void testScenario(boolean usingClassBridge) {
		FullTextSessionBuilder fulltextSessionBuilder = createSearchFactory( usingClassBridge );
		try {
			initializeData( fulltextSessionBuilder );
			FullTextSession fullTextSession = fulltextSessionBuilder.openFullTextSession();
			try {
				Catalog catalog = (Catalog) fullTextSession.load( Catalog.class, 1L );
				PersistentSet catalogItems = (PersistentSet) catalog.getCatalogItems();
				PersistentBag consumers = (PersistentBag) catalog.getConsumers();
				assertFalse( "collection catalogItems should not be initialized", catalogItems.wasInitialized() );
				assertFalse( "collection consumers should not be initialized", consumers.wasInitialized() );
				updateCatalogsCollection( fullTextSession, catalog );
				assertEquals( "collection catalogItems should not be initialized", usingClassBridge, catalogItems.wasInitialized() );
				assertTrue( "collection consumers should not be initialized", consumers.wasInitialized() );
			} finally {
				fullTextSession.close();
			}
		} finally {
			fulltextSessionBuilder.close();
		}
	}
	
	private FullTextSessionBuilder createSearchFactory(boolean defineClassBridge) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
			.addAnnotatedClass( Catalog.class )
			.addAnnotatedClass( CatalogItem.class )
			.addAnnotatedClass( Consumer.class )
			.addAnnotatedClass( Item.class );
		SearchMapping fluentMapping = builder.fluentMapping();
		fluentMapping
			.entity( Catalog.class )
				.property( "catalogItems", ElementType.FIELD ).containedIn()
			.entity( CatalogItem.class )
				.property( "item", ElementType.FIELD ).containedIn()
				.property( "catalog", ElementType.FIELD ).indexEmbedded();
		if ( defineClassBridge ) {
			fluentMapping
				.entity( Item.class )
					.classBridge( ItemClassBridge.class )
					.indexed()
					.property( "catalogItems", ElementType.FIELD ).indexEmbedded();
		}
		else {
			fluentMapping
			.entity( Item.class )
				.indexed()
				.property( "catalogItems", ElementType.FIELD ).indexEmbedded();
		}
		return builder.build();
	}

	/**
	 * Initialize the test data.
	 * @param fulltextSessionBuilder 
	 */
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

