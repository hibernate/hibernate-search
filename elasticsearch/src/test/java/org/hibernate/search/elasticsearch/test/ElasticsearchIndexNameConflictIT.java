/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link ElasticsearchIndexManager}'s indexNullAs type checking.
 *
 * @author Davide D'Alto
 */
public class ElasticsearchIndexNameConflictIT extends SearchInitializationTestBase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testNameCollisionDetection() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000326" );
		thrown.expectMessage( MyFirstEntity.class.getName() );
		thrown.expectMessage( MyfirstEntity.class.getName() );
		thrown.expectMessage( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( MyfirstEntity.class.getName() ) );

		init( MyFirstEntity.class, MyfirstEntity.class );
	}

	@Test
	public void testWithoutConflictWhenIndexIsRenamed() {
		init( MySecondEntity.class, MysecondEntity.class );
	}

	@Indexed
	@Entity
	static class MyFirstEntity {

		@DocumentId
		@Id
		Long id;

		@Field
		boolean myField;
	}

	@Indexed
	@Entity
	static class MyfirstEntity {

		@DocumentId
		@Id
		Long id;

		@Field
		boolean myField;
	}

	@Indexed(index = "should_not_have_conflict")
	@Entity
	static class MySecondEntity {

		@DocumentId
		@Id
		Long id;

		@Field
		boolean myField;
	}

	@Indexed
	@Entity
	static class MysecondEntity {

		@DocumentId
		@Id
		Long id;

		@Field
		boolean myField;
	}
}
