/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.backend.lucene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.Assert;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.impl.lucene.Changeset;
import org.hibernate.search.backend.impl.lucene.ChangesetList;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Test;

/**
 * Unit tests for the iterator functionality of org.hibernate.search.backend.impl.lucene.ChangesetList
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2015 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1769")
public class ChangeSetIteratorTest {

	@Test
	public void testOutherIteratorBeingEmpty() {
		ChangesetList list = new ChangesetList( new ArrayList<Changeset>( 2 ) );
		Iterator<LuceneWork> iterator = list.iterator();
		Assert.assertFalse( iterator.hasNext() );
	}

	@Test
	public void testOutherIteratorBeingEmptyException() {
		ChangesetList list = new ChangesetList( new ArrayList<Changeset>( 2 ) );
		Iterator<LuceneWork> iterator = list.iterator();
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
		ArrayList<Changeset> outher = new ArrayList<Changeset>( 2 );
		outher.add( new Changeset( new ArrayList<LuceneWork>(2), null, null ) );
		outher.add( new Changeset( new ArrayList<LuceneWork>(2), null, null ) );
		ChangesetList list = new ChangesetList( outher );
		Iterator<LuceneWork> iterator = list.iterator();
		Assert.assertFalse( "Iterator should have been empty", iterator.hasNext() );
	}

	@Test
	public void testInnerIteratorBeingEmptyException() {
		ArrayList<Changeset> outher = new ArrayList<Changeset>( 2 );
		outher.add( new Changeset( new ArrayList<LuceneWork>(2), null, null ) );
		outher.add( new Changeset( new ArrayList<LuceneWork>(2), null, null ) );
		ChangesetList list = new ChangesetList( outher );
		Iterator<LuceneWork> iterator = list.iterator();
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
		ArrayList<LuceneWork> workListOne = new ArrayList<LuceneWork>(2);
		workListOne.add( new PurgeAllLuceneWork(Object.class) );
		ArrayList<Changeset> outher = new ArrayList<Changeset>( 2 );
		outher.add( new Changeset( workListOne, null, null ) );
		ChangesetList list = new ChangesetList( outher );
		Iterator<LuceneWork> iterator = list.iterator();
		Assert.assertTrue( "Iterator should contain some elements", iterator.hasNext() );
		Assert.assertTrue( iterator.next() instanceof PurgeAllLuceneWork );
	}

	@Test
	public void testFourElementsIterator() {
		ArrayList<LuceneWork> workListOne = new ArrayList<LuceneWork>(2);
		workListOne.add( new PurgeAllLuceneWork(Object.class) );
		workListOne.add( new AddLuceneWork(null, null, null, null ) );
		ArrayList<LuceneWork> workListTwo = new ArrayList<LuceneWork>(2);
		workListTwo.add( new OptimizeLuceneWork(Object.class) );
		workListTwo.add( new UpdateLuceneWork(null, null, null, null ) );
		ArrayList<Changeset> outher = new ArrayList<Changeset>( 2 );
		outher.add( new Changeset( workListOne, null, null ) );
		outher.add( new Changeset( workListTwo, null, null ) );
		ChangesetList list = new ChangesetList( outher );
		Iterator<LuceneWork> iterator = list.iterator();
		Assert.assertTrue( "Iterator should contain some elements", iterator.hasNext() );
		Assert.assertTrue( iterator.next() instanceof PurgeAllLuceneWork );
		iterator.hasNext();
		Assert.assertTrue( iterator.next() instanceof AddLuceneWork );
		iterator.hasNext();
		Assert.assertTrue( iterator.next() instanceof OptimizeLuceneWork );
		iterator.hasNext();
		Assert.assertTrue( iterator.next() instanceof UpdateLuceneWork );
	}

}
