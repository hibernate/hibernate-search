/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.initandlookup;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.BytemanHelper.BytemanAccessor;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Memo to debug Byteman rules:
 * -Dorg.jboss.byteman.transform.all=true -Dorg.jboss.byteman.verbose=true -Dorg.jboss.byteman.dump.generated.classes -Dorg.jboss.byteman.dump.generated.classes.directory=/home/sanne/bytemandump
 *
 * Dumping the compiled classes makes it easier to decompile them and figure out what's wrong,
 * e.g. invoke javap -c -p -v BytemanHelper_HelperAdapter_Interpreted_1.class
 * The directory to store the dumped classes must exist already.
 */
@RunWith(BMUnitRunner.class)
public class CriteriaObjectInitializerAndHierarchyInheritanceTest extends SearchTestBase {

	@Rule
	public BytemanAccessor byteman = BytemanHelper.createAccessor();

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ BaseEntity.class, A.class, AA.class, AAA.class, AAB.class, AB.class, ABA.class, AC.class, B.class, BA.class };
	}

	@BMRule(
			name = "trackCriteriaEntityType",
			targetClass = "org.hibernate.search.query.hibernate.impl.CriteriaObjectInitializer",
			targetMethod = "buildUpCriteria(java.util.List, org.hibernate.search.query.hibernate.impl.ObjectInitializationContext)",
			targetLocation = "EXIT",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			binding = "c : org.hibernate.internal.CriteriaImpl = $!.get(0);",
			action = "pushEvent(c.getEntityOrClassName())"
		)
	@Test
	@TestForIssue(jiraKey = "HSEARCH-2301")
	@SuppressWarnings("unchecked")
	public void testJoinsAreOnlyOnUsefulEntityTypes() throws Exception {
		Session s = openSession();

		Transaction tx = s.beginTransaction();
		int i = 1;
		createInstance( s, A.class, i++, "A" );
		createInstance( s, A.class, i++, "A" );
		createInstance( s, AA.class, i++, "A AA" );
		createInstance( s, AA.class, i++, "A AA" );
		createInstance( s, AAA.class, i++, "A AA AAA" );
		createInstance( s, AAA.class, i++, "A AA AAA" );
		createInstance( s, AAA.class, i++, "A AA AAA" );
		createInstance( s, AAB.class, i++, "A AA AAB" );
		createInstance( s, AAB.class, i++, "A AA AAB" );
		createInstance( s, AB.class, i++, "A AB" );
		createInstance( s, AB.class, i++, "A AB" );
		createInstance( s, ABA.class, i++, "A AB ABA" );
		createInstance( s, ABA.class, i++, "A AB ABA" );
		createInstance( s, AC.class, i++, "A AC" );
		createInstance( s, AC.class, i++, "A AC" );
		createInstance( s, B.class, i++, "B" );
		createInstance( s, B.class, i++, "B" );
		createInstance( s, BA.class, i++, "B BA" );
		createInstance( s, BA.class, i++, "B BA" );
		tx.commit();
		s.clear();

		FullTextSession session = Search.getFullTextSession( s );

		List<?> results = getResults( session, AAA.class );
		assertThat( results ).onProperty( "name" ).containsOnly( "A AA AAA" );
		assertThat( byteman.consumeNextRecordedEvent() ).isEqualTo( AAA.class.getName() );

		results = getResults( session, AAA.class, AAB.class );
		assertThat( results ).onProperty( "name" ).containsOnly( "A AA AAA", "A AA AAB" );
		assertThat( byteman.consumeNextRecordedEvent() ).isEqualTo( AA.class.getName() );

		results = getResults( session, AAA.class, AB.class );
		assertThat( results ).onProperty( "name" ).containsOnly( "A AA AAA", "A AB", "A AB ABA" );
		assertThat( byteman.consumeNextRecordedEvent() ).isEqualTo( A.class.getName() );

		results = getResults( session, AAA.class, BA.class );
		assertThat( results ).onProperty( "name" ).containsOnly( "A AA AAA", "B BA" );
		// here, we have 2 Criterias returned: we only test the first one
		assertThat( byteman.consumeNextRecordedEvent() ).isIn( AAA.class.getName(), BA.class.getName() );

		results = getResultsFiltered( session, new MatchAllDocsQuery(), A.class );
		assertThat( byteman.consumeNextRecordedEvent() ).isEqualTo( A.class.getName() );

		// and finally we verify that if the full-text query is narrowing results to a subset of types,
		// only these are being targeted by the loading criteria.
		// First the simple case, narrowing down to a single type:
		final TermQuery termQueryAAA = new TermQuery( new Term( "name", "aaa" ) );
		results = getResultsFiltered( session, termQueryAAA, A.class );
		assertThat( byteman.consumeNextRecordedEvent() ).isEqualTo( AAA.class.getName() );

		// And then when it narrows down to two types, use a Join Criteria on the first upper shared type:
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();
		final TermQuery termQueryAAB = new TermQuery( new Term( "name", "aab" ) );
		bqb.add( termQueryAAA, Occur.SHOULD );
		bqb.add( termQueryAAB, Occur.SHOULD );
		results = getResultsFiltered( session, bqb.build(), A.class );
		assertThat( byteman.consumeNextRecordedEvent() ).isEqualTo( AA.class.getName() );

		s.close();
	}

	private void createInstance(Session s, Class<? extends BaseEntity> clazz, Integer id, String name) throws Exception {
		BaseEntity entity = clazz.newInstance();
		entity.id = id;
		entity.name = name;
		s.persist( entity );
	}

	@SuppressWarnings("unchecked")
	private List<?> getResults(FullTextSession session, Class<? extends BaseEntity>... classes) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();
		for ( Class<? extends BaseEntity> clazz : classes ) {
			bqb.add( new TermQuery( new Term( "name", clazz.getSimpleName().toLowerCase( Locale.ENGLISH ) ) ), Occur.SHOULD );
		}
		return getResultsFiltered( session, bqb.build(), BaseEntity.class );
	}

	private List<?> getResultsFiltered(FullTextSession session, Query query, Class<? extends BaseEntity>... classes) {
		return session.createFullTextQuery( query, classes )
				.setSort( new Sort( new SortField( "idSort", SortField.Type.INT ) ) )
				.list();
	}

	@MappedSuperclass
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "BaseEntity")
	public abstract static class BaseEntity {
		@Id
		@DocumentId
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		Integer id;

		@Field
		String name;

		public String getName() {
			return name;
		}
	}

	@Entity
	@Indexed
	@Table(name = "A")
	public static class A extends BaseEntity {
	}

	@Entity
	@Indexed
	@Table(name = "AA")
	public static class AA extends A {
	}

	@Entity
	@Indexed
	@Table(name = "AAA")
	public static class AAA extends AA {
	}

	@Entity
	@Indexed
	@Table(name = "AAB")
	public static class AAB extends AA {
	}

	@Entity
	@Indexed
	@Table(name = "AB")
	public static class AB extends A {
	}

	@Entity
	@Indexed
	@Table(name = "ABA")
	public static class ABA extends AB {
	}

	@Entity
	@Indexed
	@Table(name = "AC")
	public static class AC extends A {
	}

	@Entity
	@Indexed
	@Table(name = "B")
	public static class B extends BaseEntity {
	}

	@Entity
	@Indexed
	@Table(name = "BA")
	public static class BA extends B {
	}

}
