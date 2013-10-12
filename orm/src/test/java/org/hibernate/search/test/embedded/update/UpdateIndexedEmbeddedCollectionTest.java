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
package org.hibernate.search.test.embedded.update;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.TestForIssue;

/**
 * Unit test about updating an entity with collection marked with @IndexedEmbedded annotation
 *
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
public class UpdateIndexedEmbeddedCollectionTest extends SearchTestCase {

	@TestForIssue(jiraKey = "HSEARCH-734")
	public void testUpdateIndexedEmbeddedCollectionWithNull() throws Exception {

		// load the truck with number plate "LVN 746 XD" guided by driver Mark Smith
		Driver driverSmith = new Driver( "Mark", "Smith" );
		Truck truckLVN746XD = new Truck( "LVN 746 XD" );
		driverSmith.setTruck( truckLVN746XD );
		Item item1 = new Item( "Sofa", 1 );
		Item item2 = new Item( "Table", 3 );
		Item item3 = new Item( "Chair", 24 );
		Set<Item> itemsTruckLVN746XD = new HashSet<Item>();
		itemsTruckLVN746XD.add( item1 );
		itemsTruckLVN746XD.add( item2 );
		itemsTruckLVN746XD.add( item3 );
		truckLVN746XD.setItems( itemsTruckLVN746XD );
		// load the truck with number plate "MLN 666 DJ" guided by driver John Doe
		Driver driverDoe = new Driver( "John", "Doe" );
		Truck truckMLN666DJ = new Truck( "MLN 666 DJ" );
		driverDoe.setTruck( truckMLN666DJ );
		Item item4 = new Item( "Armchair", 8 );
		Item item5 = new Item( "Chair", 19 );
		Set<Item> itemsTruckMLN666DJ = new HashSet<Item>();
		itemsTruckMLN666DJ.add( item4 );
		itemsTruckMLN666DJ.add( item5 );
		truckMLN666DJ.setItems( itemsTruckMLN666DJ );

		// first operation -> save
		FullTextSession session = Search.getFullTextSession( openSession() );
		Transaction tx = session.beginTransaction();
		session.save( driverSmith );
		session.save( driverDoe );
		tx.commit();
		session.close();

		// assert that everything got saved and indexed correctly
		final Long truckLVN746XDId = truckLVN746XD.getId();
		final Long truckMLN666DJId = truckMLN666DJ.getId();
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertEquals( truckMLN666DJId, findTruckIdFromIndex( session, "armchair" ) );
		assertEquals( truckLVN746XDId, findTruckIdFromIndex( session, "table" ) );
		tx.commit();
		session.close();

		// now unload the truck "LVN 746 XD" because his driver, Mark Smith, arrived at his destination.
		truckLVN746XD.setItems( null );
		// let's update
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		session.update( driverSmith );
		// if the Hibernate-Search release is 3.4.0.CR2, it throws NullPointerException described in HSEARCH-734 during
		// commit phase
		tx.commit();
		session.close();

		// let's assert that indexes got updated correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertNull( findTruckIdFromIndex( session, "table" ) );
		tx.commit();
		session.close();

		// Now let's clean up everything
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		session.delete( driverSmith );
		session.delete( driverDoe );
		tx.commit();
		session.close();
	}

	private Long findTruckIdFromIndex(FullTextSession session, String itemDescription) {
		FullTextQuery q = session.createFullTextQuery(
				new TermQuery( new Term( "truck.items.description", itemDescription ) ), Driver.class );
		q.setProjection( "truck.id" );
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.list();
		if ( results.isEmpty() ) {
			return null;
		}
		return (Long) results.get( 0 )[0];
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Driver.class, Truck.class, Item.class };
	}

	@Entity(name = "Driver")
	@Indexed
	public static class Driver {

		public Driver(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public Driver() {
		}

		@Id @GeneratedValue @DocumentId
		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
		private Long id;

		@Field
		public String getFirstName() { return firstName; }
		public void setFirstName(String firstName) { this.firstName = firstName; }
		private String firstName;

		@Field
		public String getLastName() { return lastName; }
		public void setLastName(String lastName) { this.lastName = lastName; }
		private String lastName;

		@IndexedEmbedded @OneToOne(cascade = CascadeType.ALL)
		public Truck getTruck() { return truck; }
		public void setTruck(Truck truck) { this.truck = truck; }
		private Truck truck;
	}

	@Entity(name = "Truck")
	@Indexed
	public static class Truck {

		public Truck(String numberPlate) {
			super();
			this.numberPlate = numberPlate;
		}

		public Truck() {
		}

		@Id @GeneratedValue @DocumentId
		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
		private Long id;

		@Field
		public String getNumberPlate() { return numberPlate; }
		public void setNumberPlate(String numberPlate) { this.numberPlate = numberPlate; }
		private String numberPlate;

		@IndexedEmbedded @OneToMany(cascade = CascadeType.ALL)
		public Set<Item> getItems() { return items; }
		public void setItems(Set<Item> items) { this.items = items; }
		private Set<Item> items;
	}

	@Entity(name = "Item")
	@Indexed
	public static class Item {
		public Item(String description, Integer quantity) {
			super();
			this.description = description;
			this.quantity = quantity;
		}

		public Item() {
		}

		@Id @GeneratedValue @DocumentId
		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
		private Long id;

		@Field
		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }
		private String description;

		@Field
		public Integer getQuantity() { return quantity; }
		public void setQuantity(Integer quantity) { this.quantity = quantity; }
		private Integer quantity;
	}

}
