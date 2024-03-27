/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.polymorphism;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

@TestForIssue(jiraKey = "HSEARCH-3156")
class PolymorphicAssociationContainedInTargetTest extends SearchTestBase {

	private static final String INIT_NAME = "initname";
	private static final String EDIT_NAME = "editname";

	// Nominal case: the level3 refers to a level2 of a configured type (DerivedLevel2).
	@Test
	void testPolymorphicAssociationConfiguredType() {
		try ( Session session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			Level1 level1 = new Level1();
			level1.setId( 1 );

			DerivedLevel2 derivedLevel2 = new DerivedLevel2();
			derivedLevel2.setId( 2 );
			Level3 derivedLevel2Level3 = new Level3();
			derivedLevel2Level3.setId( 3 );
			derivedLevel2Level3.setName( INIT_NAME );

			level1.setDerivedLevel2Child( derivedLevel2 );
			derivedLevel2.setLevel1Parent( level1 );
			derivedLevel2.setLevel3Child( derivedLevel2Level3 );
			derivedLevel2Level3.setLevel2Parent( derivedLevel2 );

			session.save( level1 );
			session.save( derivedLevel2 );
			session.save( derivedLevel2Level3 );

			transaction.commit();
		}

		try ( Session session = openSession();
				FullTextSession fullTextSession = Search.getFullTextSession( session ) ) {
			Transaction transaction = fullTextSession.beginTransaction();

			Level3 derivedLevel2Level3 = session.get( Level3.class, 3 );
			derivedLevel2Level3.setName( EDIT_NAME );
			transaction.commit();
		}

		try ( Session session = openSession();
				FullTextSession fullTextSession = Search.getFullTextSession( session ) ) {
			Query q = new TermQuery( new Term( "derivedLevel2Child.level3Child.name", EDIT_NAME ) );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q );
			assertThat( fullTextQuery.getResultSize() ).isEqualTo( 1 );
		}
	}

	// Failing case: the level3 refers to a level2 of a non-configured type (Level2, the base type).
	// This should not affect the index, but indexing used to fail with an exception before HSEARCH-3156 was solved.
	@Test
	void testPolymorphicAssociationNonConfiguredType() {
		try ( Session session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			Level1 level1 = new Level1();
			level1.setId( 1 );

			Level2 baseLevel2 = new Level2();
			baseLevel2.setId( 4 );
			Level3 baseLevel2Level3 = new Level3();
			baseLevel2Level3.setId( 5 );
			baseLevel2Level3.setName( INIT_NAME );

			baseLevel2.setLevel3Child( baseLevel2Level3 );
			baseLevel2Level3.setLevel2Parent( baseLevel2 );

			session.save( level1 );
			session.save( baseLevel2 );
			session.save( baseLevel2Level3 );

			transaction.commit();
		}

		try ( Session session = openSession();
				FullTextSession fullTextSession = Search.getFullTextSession( session ) ) {
			Transaction transaction = fullTextSession.beginTransaction();

			Level3 baseLevel2Level3 = session.get( Level3.class, 5 );
			baseLevel2Level3.setName( EDIT_NAME );
			transaction.commit();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Level1.class, Level2.class, DerivedLevel2.class, Level3.class };
	}

	@Entity(name = "Level1")
	@Indexed
	static class Level1 {

		@Id
		@DocumentId
		private Integer id;

		@OneToOne(mappedBy = "level1Parent")
		@IndexedEmbedded
		private DerivedLevel2 derivedLevel2Child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public DerivedLevel2 getDerivedLevel2Child() {
			return derivedLevel2Child;
		}

		public void setDerivedLevel2Child(DerivedLevel2 derivedLevel2Child) {
			this.derivedLevel2Child = derivedLevel2Child;
		}
	}

	@Entity(name = "Level2")
	static class Level2 {

		@Id
		private Integer id;

		@OneToOne(mappedBy = "level2Parent")
		@IndexedEmbedded
		private Level3 level3Child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level3 getLevel3Child() {
			return level3Child;
		}

		public void setLevel3Child(Level3 level3Child) {
			this.level3Child = level3Child;
		}

	}

	@Entity(name = "DerivedLevel2")
	static class DerivedLevel2 extends Level2 {

		@OneToOne
		private Level1 level1Parent;

		public Level1 getLevel1Parent() {
			return level1Parent;
		}

		public void setLevel1Parent(Level1 level1Parent) {
			this.level1Parent = level1Parent;
		}

	}

	@Entity(name = "Level3")
	static class Level3 {

		@Id
		private Integer id;

		@Field
		private String name;

		@OneToOne(fetch = FetchType.LAZY)
		private Level2 level2Parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Level2 getLevel2Parent() {
			return level2Parent;
		}

		public void setLevel2Parent(Level2 level2Parent) {
			this.level2Parent = level2Parent;
		}

	}
}
