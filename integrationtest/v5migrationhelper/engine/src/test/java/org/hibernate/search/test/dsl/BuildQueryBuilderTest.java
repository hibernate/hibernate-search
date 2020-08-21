/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2521")
public class BuildQueryBuilderTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder(
			ConfiguredNotIndexed.class, ConfiguredIndexed.class );

	@Test
	public void forEntity_configured_indexed() {
		QueryBuilder builder = sfHolder.getSearchFactory().buildQueryBuilder().forEntity( ConfiguredIndexed.class ).get();
		Assert.assertEquals( new MatchAllDocsQuery(), builder.all().createQuery() );
	}

	@Test
	public void forEntity_configured_notIndexed() {
		thrown.expectMessage( "HSEARCH000278" );
		thrown.expectMessage( "indexed" );
		thrown.expectMessage( ConfiguredNotIndexed.class.getSimpleName() );
		sfHolder.getSearchFactory().buildQueryBuilder().forEntity( ConfiguredNotIndexed.class ).get();
	}

	@Test
	public void forEntity_notConfigured_indexed() {
		thrown.expectMessage( "HSEARCH000331" );
		thrown.expectMessage( "configured" );
		thrown.expectMessage( NotConfiguredIndexed.class.getSimpleName() );
		sfHolder.getSearchFactory().buildQueryBuilder().forEntity( NotConfiguredIndexed.class ).get();
	}

	@Test
	public void forEntity_notConfigured_notIndexed() {
		thrown.expectMessage( "HSEARCH000331" );
		thrown.expectMessage( "configured" );
		thrown.expectMessage( NotConfiguredNotIndexed.class.getSimpleName() );
		sfHolder.getSearchFactory().buildQueryBuilder().forEntity( NotConfiguredNotIndexed.class ).get();
	}

	private static class ConfiguredNotIndexed {
		@DocumentId
		private int id;

		@Field
		private String field;
	}

	@Indexed
	private static class ConfiguredIndexed {
		@DocumentId
		private int id;

		@Field
		private String field;
	}

	private static class NotConfiguredNotIndexed {
		@DocumentId
		private int id;

		@Field
		private String field;
	}

	@Indexed
	private static class NotConfiguredIndexed {
		@DocumentId
		private int id;

		@Field
		private String field;
	}
}
