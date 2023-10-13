/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.path.multiple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;

/**
 * @author Davide D'Alto
 */
class MultiplePathCaseEmbeddedTest extends SearchTestBase {

	private Session s = null;
	private EntityA entityA = null;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		EntityC indexedC = new EntityC( "indexed" );
		indexedC.anotherField = "anotherField";
		EntityC skippedC = new EntityC( "indexed" );

		EntityB indexedB = new EntityB( indexedC, skippedC );

		entityA = new EntityA( indexedB );
		s = openSession();
		persistEntity( s, indexedC, skippedC, indexedB, entityA );
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		s.clear();

		deleteAll( s, EntityA.class, EntityB.class, EntityC.class );
		s.close();
		super.tearDown();
	}

	@Test
	void testRenamedFieldInFieldsIsIndexedIfInPath() {
		List<EntityA> result = search( s, "b.indexed.renamed", "indexed" );

		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ).id ).isEqualTo( entityA.id );
	}

	@Test
	void testAnotherFieldIsIndexedIfInPath() {
		List<EntityA> result = search( s, "b.indexed.anotherField", "anotherField" );

		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ).id ).isEqualTo( entityA.id );
	}

	@Test
	void testFieldNotIndexedIfInPathWithAttributeName() {
		assertThatThrownBy( () -> search( s, "b.indexed.field", "indexed" ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void testRenamedFieldNotIndexedIfInNotPath() {
		assertThatThrownBy( () -> search( s, "b.indexed.renamedSkipped", "indexed" ) )
				.isInstanceOf( SearchException.class );
	}


	@Test
	void testEmbeddedNotIndexedIfNotInPath() {
		assertThatThrownBy( () -> search( s, "b.skipped.indexed", "indexed" ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void testFieldNotIndexedIfNotInPath() {
		assertThatThrownBy( () -> search( s, "b.indexed.skipped", "skipped" ) )
				.isInstanceOf( SearchException.class );
	}

	private List<EntityA> search(Session s, String field, String value) {
		FullTextSession session = Search.getFullTextSession( s );
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( EntityA.class ).get();
		Query query = queryBuilder.keyword().onField( field ).matching( value ).createQuery();
		@SuppressWarnings("unchecked")
		List<EntityA> result = session.createFullTextQuery( query ).list();
		return result;
	}

	private void deleteAll(Session s, Class<?>... classes) {
		Transaction tx = s.beginTransaction();
		for ( Class<?> each : classes ) {
			List<?> list = listAll( s, each );
			for ( Object object : list ) {
				s.delete( object );
			}
		}
		tx.commit();
	}

	private void persistEntity(Session s, Object... entities) {
		Transaction tx = s.beginTransaction();
		for ( Object entity : entities ) {
			s.persist( entity );
		}
		tx.commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class, EntityC.class };
	}
}
