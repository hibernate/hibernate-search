/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.nested.containedIn;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;

import org.junit.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * @author Emmanuel Bernard
 */
public class NestedContainedInTest extends SearchTestBase {

	@Test
	public void testAddHelpItem() {
		openSession();
		String tagName = "animal";
		createHelpItem( tagName );
		doQuery( tagName );
		getSession().close();
	}

	@Test
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
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				HelpItem.class,
				HelpItemTag.class,
				Tag.class
		};
	}
}
