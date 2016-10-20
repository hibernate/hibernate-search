/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link ElasticsearchIndexManager}'s indexNullAs type checking.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexNullAsTypeCheckingIT extends SearchInitializationTestBase {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void indexNullAs_invalid_boolean() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400027" );
		thrown.expectMessage( "Boolean" );
		thrown.expectMessage( "myField" );

		init( BooleanFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class BooleanFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "foo")
		boolean myField;
	}

	@Test
	public void indexNullAs_invalid_date() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400028" );
		thrown.expectMessage( "Date" );
		thrown.expectMessage( "myField" );

		init( DateFailureTestEntity.class );
	}

	@Indexed
	@Entity
	public static class DateFailureTestEntity {
		@DocumentId
		@Id
		Long id;

		@Field(indexNullAs = "01/01/2013") // Expected format is ISO-8601 (yyyy-MM-dd'T'HH:mm:ssZ)
		Date myField;
	}

}
