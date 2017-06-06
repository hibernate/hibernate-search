/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.fest.assertions.Assertions;
import org.fest.assertions.ListAssert;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.util.impl.CollectionHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the iterator functionality of {@link CollectionHelper#flatten(Iterable)}
 * and {@link CollectionHelper#flatten(Iterator)}.
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1769")
public class IterableFlatteningTest {

	@Test
	public void testOuterIteratorBeingEmpty() {
		List<Iterable<Integer>> outer = Arrays.asList();
		Iterable<Integer> flattened = CollectionHelper.flatten( outer );
		assertIterable( flattened ).isEmpty();
	}

	@Test
	public void testOuterIteratorBeingEmptyException() {
		List<Iterable<Integer>> outer = Arrays.asList();
		Iterable<Integer> flattened = CollectionHelper.flatten( outer );
		Iterator<Integer> iterator = flattened.iterator();
		try {
			iterator.next();
			Assert.fail( "Should have thrown the exception" );
		}
		catch (NoSuchElementException e) {
			//All good
		}
	}

	@Test
	public void testInnerIteratorBeingEmpty() {
		List<Iterable<Integer>> outer = Arrays.asList( Arrays.asList(), Arrays.asList() );
		Iterable<Integer> flattened = CollectionHelper.flatten( outer );
		assertIterable( flattened ).isEmpty();
	}

	@Test
	public void testInnerIteratorBeingEmptyException() {
		List<Iterable<Integer>> outer = Arrays.asList( Arrays.asList(), Arrays.asList() );
		Iterable<Integer> flattened = CollectionHelper.flatten( outer );
		Iterator<Integer> iterator = flattened.iterator();
		try {
			iterator.next();
			Assert.fail( "Should have thrown the exception" );
		}
		catch (NoSuchElementException e) {
			//All good
		}
	}

	@Test
	public void testInnerIteratorSingleEmpty() {
		List<Integer> workListOne = Arrays.asList( 1 );
		List<Iterable<Integer>> outer = Arrays.asList( workListOne );
		Iterable<Integer> flattened = CollectionHelper.flatten( outer );
		assertIterable( flattened ).containsExactly( 1 );
	}

	@Test
	public void testFourElementsIterator() {
		List<Integer> workListOne = Arrays.asList( 1, 2 );
		List<Integer> workListTwo = Arrays.asList( 3, 4 );
		List<Iterable<Integer>> outer = Arrays.asList( workListOne, workListTwo );
		Iterable<Integer> flattened = CollectionHelper.flatten( outer );
		assertIterable( flattened ).containsExactly( 1, 2, 3, 4 );
	}

	private static <T> ListAssert assertIterable(Iterable<T> iterable) {
		Iterator<T> iterator = iterable == null ? null : iterable.iterator();
		if ( iterator == null ) {
			return Assertions.assertThat( (List<T>) null );
		}
		List<T> list = new ArrayList<>();
		iterator.forEachRemaining( list::add );
		return Assertions.assertThat( list );
	}

}
