/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.graph;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.search.test.SearchTestBase;

import org.junit.jupiter.api.Test;

/**
 * TestCase to verify proper management of saving of complex relations and collections. See HSEARCH-476
 */
class RecursiveGraphTest extends SearchTestBase {

	@Test
	void testCreateParentAndChild() {
		Person[] people = new Person[2];
		Person parent = new Person();
		parent.setName( "parent" );
		Person child = new Person();
		child.setName( "child" );
		connectChildToParent( child, parent );
		people[0] = parent;
		people[1] = child;
		savePeople( people );
		assertThat( getNumberOfDocumentsInIndex( "Person" ) ).isEqualTo( 2 );
	}

	private void connectChildToParent(Person child, Person parent) {
		Event birthEvent = child.getBirthEvent();
		child.setBirthEvent( birthEvent );
		ParentOfBirthEvent parentOfBirthEvent = new ParentOfBirthEvent( parent, child.getBirthEvent() );
		parent.getParentOfBirthEvents().add( parentOfBirthEvent );
	}

	public void savePeople(Person... people) {
		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		for ( Person person : people ) {
			if ( person == null ) {
				continue;
			}
			s.save( person );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Event.class, Person.class, ParentOfBirthEvent.class };
	}
}
