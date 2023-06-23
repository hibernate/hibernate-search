/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.graph;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.search.test.SearchTestBase;

import org.junit.Test;

/**
 * TestCase to verify proper management of saving of complex relations and collections. See HSEARCH-476
 */
public class RecursiveGraphTest extends SearchTestBase {

	@Test
	public void testCreateParentAndChild() throws Exception {
		Person[] people = new Person[2];
		Person parent = new Person();
		parent.setName( "parent" );
		Person child = new Person();
		child.setName( "child" );
		connectChildToParent( child, parent );
		people[0] = parent;
		people[1] = child;
		savePeople( people );
		assertEquals( 2, getNumberOfDocumentsInIndex( "Person" ) );
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
