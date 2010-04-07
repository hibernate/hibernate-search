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
		String tagName = "animal";
		createHelpItem( tagName );
		openSession(  );
		doQuery( tagName );
		session.close();
	}

	public void testChangeTagName() {

		String tagName = "animal";
		createHelpItem( tagName );

		openSession(  );
		HelpItem check = doQuery( tagName );

		Tag tag = check.getTags().get( 0 ).getTag();

		Transaction tx = session.beginTransaction();
		String newTagName = "automobile";
		tag.setName( newTagName );
		session.saveOrUpdate( tag );
		tx.commit();

		doQuery( newTagName );
		session.close();
	}

	private void createHelpItem(String tagName) {
		openSession(  );
		Transaction tx = session.beginTransaction();
		HelpItem helpItem = new HelpItem();
		helpItem.setTitle( "The quick brown fox jumps over the lazy dog." );

		Tag tag = new Tag();
		tag.setName( tagName );

		HelpItemTag helpItemTag = new HelpItemTag();
		helpItemTag.setHelpItem( helpItem );
		helpItemTag.setTag( tag );

		helpItem.getTags().add( helpItemTag );
		tag.getHelpItems().add( helpItemTag );

		session.save( helpItem );
		session.save( tag );
		session.save( helpItemTag );

		tx.commit();
		session.close();
	}

	private HelpItem doQuery(String tagName) {
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Query termQuery = new TermQuery( new Term( "tags.tag.name", tagName ) );
		FullTextQuery fullTextQuery =
				fullTextSession.createFullTextQuery( termQuery, HelpItem.class );
		HelpItem check = ( HelpItem ) fullTextQuery.uniqueResult();
		assertNotNull( "No HelpItem with Tag '" + tagName + "' found in Lucene index.", check );
		assertTrue( check.getTags().get( 0 ).getTag().getName().equals( tagName ) );
		return check;
	}

	@Override
	protected Class<?>[] getMappings() {
		return new Class<?>[] {
				HelpItem.class,
				HelpItemTag.class,
				Tag.class
		};
	}
}
