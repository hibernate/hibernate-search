/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.serialization;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.testsupport.serialization.SerializationTestHelperTest.Foo.TestInnerClass;
import org.junit.Test;

/**
 * @author Sanne Grinovero
 */
public class SerializationTestHelperTest {

	@Test
	public void duplicatesAreEqual() throws IOException, ClassNotFoundException {
		Foo a = new Foo();
		a.list.add( new TestInnerClass( 30 ) );
		Foo b = (Foo) SerializationTestHelper.duplicateBySerialization( a );
		assertEquals( Integer.valueOf( 6 ), a.integer );
		assertEquals( Integer.valueOf( 7 ), b.integer );
		assertEquals( a.list, b.list );
	}

	static class Foo implements Serializable {

		List<TestInnerClass> list = new ArrayList<TestInnerClass>();
		transient Integer integer = Integer.valueOf( 6 );

		static class TestInnerClass implements Serializable {
			private final int v;

			public TestInnerClass(int i) {
				v = i;
			}

			public void print() {
				System.out.println( v );
			}

			@Override
			public String toString() {
				return "" + v;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + v;
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if ( this == obj ) {
					return true;
				}
				if ( obj == null ) {
					return false;
				}
				if ( getClass() != obj.getClass() ) {
					return false;
				}
				final TestInnerClass other = (TestInnerClass) obj;
				if ( v != other.v ) {
					return false;
				}
				return true;
			}
		}

		private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
			aInputStream.defaultReadObject();
			integer = Integer.valueOf( 7 );
		}

		private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
			aOutputStream.defaultWriteObject();
		}
	}

}
