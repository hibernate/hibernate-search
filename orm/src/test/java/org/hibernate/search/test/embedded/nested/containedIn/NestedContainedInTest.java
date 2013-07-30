/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.embedded.nested.containedIn;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class NestedContainedInTest extends SearchTestCase {

	public void testAddHelpItem() {
		openSession();
		String tagName = "animal";
		createHelpItem( tagName );
		doQuery( tagName );
		getSession().close();
	}

	public void testChangeTagName() {
		openSession();
		String tagName = "animal";
		createHelpItem( tagName );

		HelpItem check = doQuery( tagName );
		Tag tag = check.getTags().get( 0 ).getTag();

		String newTagName = "automobile";
		tag.setName( newTagName );

		Transaction tx = getSession().beginTransaction();
		getSession().saveOrUpdate( tag );
		tx.commit();

		doQuery( newTagName );
		getSession().close();
	}

	private void createHelpItem(String tagName) {
		Transaction tx = getSession().beginTransaction();
		HelpItem helpItem = new HelpItem();
		helpItem.setTitle( "The quick brown fox jumps over the lazy dog." );

		Tag tag = new Tag();
		tag.setName( tagName );

		HelpItemTag helpItemTag = new HelpItemTag();
		helpItemTag.setHelpItem( helpItem );
		helpItemTag.setTag( tag );

		helpItem.getTags().add( helpItemTag );
		tag.getHelpItems().add( helpItemTag );

		getSession().save( helpItem );
		getSession().save( tag );
		getSession().save( helpItemTag );

		tx.commit();
	}

	private HelpItem doQuery(String tagName) {
		Transaction tx = getSession().beginTransaction();
		FullTextSession fullTextSession = Search.getFullTextSession( getSession() );
		Query termQuery = new TermQuery( new Term( "tags.tag.name", tagName ) );
		FullTextQuery fullTextQuery =
				fullTextSession.createFullTextQuery( termQuery, HelpItem.class );
		HelpItem check = (HelpItem) fullTextQuery.uniqueResult();
		assertNotNull( "No HelpItem with Tag '" + tagName + "' found in Lucene index.", check );
		assertTrue( check.getTags().get( 0 ).getTag().getName().equals( tagName ) );
		tx.commit();
		return check;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				HelpItem.class,
				HelpItemTag.class,
				Tag.class
		};
	}
}
