/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.update;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Locale;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HSEARCH-1573")
public class ContainedInWithoutIndexedEmbeddedReindexPropagationTest extends SearchTestBase {

	@Test
	public void testUpdatingContainedInEntityPropagatesToAllEntitiesSimpleCase() throws Exception {
		// first operation -> save
		FullTextSession session = Search.getFullTextSession( openSession() );
		Transaction tx = session.beginTransaction();

		SimpleParentEntity parent = new SimpleParentEntity( "name1" );
		session.save( parent );

		SimpleChildEntity child = new SimpleChildEntity( parent );
		session.save( child );

		parent.setChild( child );
		session.update( parent );

		tx.commit();
		session.close();

		// assert that everything got saved correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertEquals( 1, getSimpleChildEntitiesFromIndex( session, parent.getName() ).size() );
		tx.commit();
		session.close();

		// update the parent name
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		parent.setName( "newname2" );
		session.update( parent );
		tx.commit();
		session.close();

		// check that the SimpleChildEntity has been reindexed correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertEquals( 1, getSimpleChildEntitiesFromIndex( session, parent.getName() ).size() );
		tx.commit();
		session.close();
	}

	@Test
	public void testUpdatingContainedInEntityPropagatesToAllEntitiesBusinessCase() throws Exception {
		ProductModel model = new ProductModel( "042024N" );

		ProductArticle article1 = new ProductArticle( model, "02" );
		ProductArticle article2 = new ProductArticle( model, "E3" );

		ProductShootingBrief shootingBrief1 = new ProductShootingBrief( "Shooting brief 1" );
		ProductShootingBrief shootingBrief2 = new ProductShootingBrief( "Shooting brief 2" );

		// first operation -> save
		FullTextSession session = Search.getFullTextSession( openSession() );
		Transaction tx = session.beginTransaction();

		session.save( shootingBrief1 );
		session.save( shootingBrief2 );

		model.setShootingBrief( shootingBrief1 );
		article2.setShootingBrief( shootingBrief2 );

		session.save( model );
		session.save( article1 );
		session.save( article2 );

		tx.commit();
		session.close();

		// assert that everything got saved correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertEquals( 2, getShootingBriefsFromIndex( session, model.getMainReferenceCode().getRawValue() ).size() );
		tx.commit();
		session.close();

		// add a new reference code to the model: this should also trigger a reindex of the ProductShootingBrief
		// due to the @ContainedIn dependency graph
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		model.getAdditionalReferenceCodes().add( new ProductReferenceCode( model, "NEWREF" ) );
		session.update( model );
		tx.commit();
		session.close();

		// check that the ProductShootingBrief has been reindexed correctly.
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertEquals( 1, getShootingBriefsFromIndex( session, "NEWREF" ).size() );
		tx.commit();
		session.close();
	}

	private List<SimpleChildEntity> getSimpleChildEntitiesFromIndex(FullTextSession session, String name) {
		FullTextQuery q =
				session.createFullTextQuery( new TermQuery( new Term( "parentName", name ) ), SimpleChildEntity.class );
		@SuppressWarnings("unchecked")
		List<SimpleChildEntity> results = q.list();
		return results;
	}

	private List<ProductShootingBrief> getShootingBriefsFromIndex(FullTextSession session, String referenceCode) {
		FullTextQuery q = session.createFullTextQuery( new WildcardQuery( new Term( "referenceCodeCollection",
				referenceCode.toLowerCase( Locale.ROOT ) + "*" ) ),
				ProductShootingBrief.class );
		@SuppressWarnings("unchecked")
		List<ProductShootingBrief> results = q.list();
		return results;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				SimpleParentEntity.class,
				SimpleChildEntity.class,
				ProductArticle.class,
				ProductModel.class,
				ProductReferenceCode.class,
				ProductShootingBrief.class };
	}
}
