/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.update;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HSEARCH-1573")
class ContainedInWithoutIndexedEmbeddedReindexPropagationTest extends SearchTestBase {

	@Test
	void testUpdatingContainedInEntityPropagatesToAllEntitiesSimpleCase() {
		// first operation -> save
		FullTextSession session = Search.getFullTextSession( openSession() );
		Transaction tx = session.beginTransaction();

		SimpleParentEntity parent = new SimpleParentEntity( "name1" );
		session.persist( parent );

		SimpleChildEntity child = new SimpleChildEntity( parent );
		session.persist( child );

		parent.setChild( child );

		tx.commit();
		session.close();

		// assert that everything got saved correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertThat( getSimpleChildEntitiesFromIndex( session, parent.getName() ) ).hasSize( 1 );
		tx.commit();
		session.close();

		// update the parent name
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		parent.setName( "newname2" );
		session.merge( parent );
		tx.commit();
		session.close();

		// check that the SimpleChildEntity has been reindexed correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertThat( getSimpleChildEntitiesFromIndex( session, parent.getName() ) ).hasSize( 1 );
		tx.commit();
		session.close();
	}

	@Test
	void testUpdatingContainedInEntityPropagatesToAllEntitiesBusinessCase() throws Exception {
		ProductModel model = new ProductModel( "042024N" );

		ProductArticle article1 = new ProductArticle( model, "02" );
		ProductArticle article2 = new ProductArticle( model, "E3" );

		ProductShootingBrief shootingBrief1 = new ProductShootingBrief( "Shooting brief 1" );
		ProductShootingBrief shootingBrief2 = new ProductShootingBrief( "Shooting brief 2" );

		// first operation -> save
		FullTextSession session = Search.getFullTextSession( openSession() );
		Transaction tx = session.beginTransaction();

		session.persist( shootingBrief1 );
		session.persist( shootingBrief2 );

		model.setShootingBrief( shootingBrief1 );
		article2.setShootingBrief( shootingBrief2 );

		session.persist( model );
		session.persist( article1 );
		session.persist( article2 );

		tx.commit();
		session.close();

		// assert that everything got saved correctly
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertThat( getShootingBriefsFromIndex( session, model.getMainReferenceCode().getRawValue() ) ).hasSize( 2 );
		tx.commit();
		session.close();

		// add a new reference code to the model: this should also trigger a reindex of the ProductShootingBrief
		// due to the @ContainedIn dependency graph
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		model.getAdditionalReferenceCodes().add( new ProductReferenceCode( model, "NEWREF" ) );
		session.merge( model );
		tx.commit();
		session.close();

		// check that the ProductShootingBrief has been reindexed correctly.
		session = Search.getFullTextSession( openSession() );
		tx = session.beginTransaction();
		assertThat( getShootingBriefsFromIndex( session, "NEWREF" ) ).hasSize( 1 );
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
