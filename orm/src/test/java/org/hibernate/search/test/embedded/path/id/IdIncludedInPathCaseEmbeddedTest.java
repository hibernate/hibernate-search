/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.path.id;

import java.util.List;

import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link org.hibernate.search.annotations.IndexedEmbedded#includePaths()}
 * should consider fields annotated with {@link javax.persistence.Id}
 * or {@link org.hibernate.search.annotations.DocumentId}.
 *
 * @author Davide D'Alto
 */
public class IdIncludedInPathCaseEmbeddedTest extends SearchTestBase {

	private Session s = null;
	private EntityA entityA = null;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		EntityC indexedC = new EntityC( "indexedId", "indexed" );
		EntityC skippedC = new EntityC( "skippedId", "indexed" );

		DocumentEntity documentEntity = new DocumentEntity( "indexedDocumentId" );
		documentEntity.c = indexedC;
		indexedC.document = documentEntity;

		EntityB indexedB = new EntityB( indexedC, skippedC );

		entityA = new EntityA( indexedB );
		s = openSession();
		persistEntity( s, documentEntity, indexedC, skippedC, indexedB, entityA );
	}

	@Override
	@After
	public void tearDown() throws Exception {
		s.clear();

		deleteAll( s, getAnnotatedClasses() );
		s.close();
		super.tearDown();
	}

	@Test
	public void testIdAttributeIndexedIfInPath() throws Exception {
		List<EntityA> result = search( s, "b.indexed.id", "indexedId" );

		Assert.assertEquals( 1, result.size() );
		Assert.assertEquals( entityA.id, result.get( 0 ).id );
	}

	@Test
	public void testDocumentIdIsIndexedIfInPath() throws Exception {
		List<EntityA> result = search( s, "b.indexed.document.documentId", "indexedDocumentId" );

		Assert.assertEquals( 1, result.size() );
		Assert.assertEquals( entityA.id, result.get( 0 ).id );
	}

	@Test
	public void testEmbeddedNotIndexedIfNotInPath() throws Exception {
		try {
			search( s, "b.skipped.skippedId", "skippedId" );
			fail( "Should not index embedded property if not in path and not in depth limit" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "Unable to find field" ) );
		}
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
			List<?> list = s.createCriteria( each ).list();
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class, EntityC.class, DocumentEntity.class };
	}
}
