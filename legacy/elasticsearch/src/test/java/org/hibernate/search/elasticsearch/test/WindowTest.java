/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hibernate.search.elasticsearch.util.impl.Window;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class WindowTest {

	@Test
	public void add_withinBounds() {
		Window<Integer> window = new Window<>( 4100, 100 );
		window.add( 1 );
		assertEquals( Integer.valueOf( 1 ), window.get( 4100 ) );
		assertEquals( 4100, window.start() );
		assertEquals( 1, window.size() );
		assertFalse( window.isEmpty() );
		assertEquals( 100, window.capacity() );

		window.add( 2 );
		assertEquals( Integer.valueOf( 1 ), window.get( 4100 ) );
		assertEquals( Integer.valueOf( 2 ), window.get( 4101 ) );
		assertEquals( 4100, window.start() );
		assertEquals( 2, window.size() );
		assertFalse( window.isEmpty() );
		assertEquals( 100, window.capacity() );
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void add_outOfBounds_after() {
		Window<Integer> window = new Window<>( 4100, 10 );
		window.add( 1 );
		window.add( 2 );

		window.get( 3 );
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void add_outOfBounds_before() {
		Window<Integer> window = new Window<>( 4100, 10 );
		window.add( 1 );
		window.add( 2 );

		window.get( 0 );
	}

	@Test
	public void add_overflow() {
		Window<Integer> window = new Window<>( 10_000, 4 );
		window.add( 1 );
		window.add( 2 );
		window.add( 3 );
		window.add( 4 );
		assertEquals( Integer.valueOf( 1 ), window.get( 10_000 ) );
		assertEquals( Integer.valueOf( 2 ), window.get( 10_001 ) );
		assertEquals( Integer.valueOf( 3 ), window.get( 10_002 ) );
		assertEquals( Integer.valueOf( 4 ), window.get( 10_003 ) );
		assertEquals( 10_000, window.start() );
		assertEquals( 4, window.size() );
		assertFalse( window.isEmpty() );
		assertEquals( 4, window.capacity() );

		window.add( 5 );
		assertEquals( Integer.valueOf( 2 ), window.get( 10_001 ) );
		assertEquals( Integer.valueOf( 3 ), window.get( 10_002 ) );
		assertEquals( Integer.valueOf( 4 ), window.get( 10_003 ) );
		assertEquals( Integer.valueOf( 5 ), window.get( 10_004 ) );
		assertEquals( 10_001, window.start() );
		assertEquals( 4, window.size() );
		assertFalse( window.isEmpty() );
		assertEquals( 4, window.capacity() );
	}

	@Test
	public void clear() {
		Window<Integer> window = new Window<>( 12, 10 );
		window.add( 1 );
		window.add( 2 );
		assertEquals( 2, window.size() );
		assertFalse( window.isEmpty() );
		assertEquals( 12, window.start() );
		assertEquals( 10, window.capacity() );

		window.clear();
		assertEquals( 0, window.size() );
		assertTrue( window.isEmpty() );
		assertEquals( 12, window.start() );
		assertEquals( 10, window.capacity() );
	}

}
