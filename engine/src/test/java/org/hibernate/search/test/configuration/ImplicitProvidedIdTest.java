/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;
import java.util.Arrays;

import org.junit.Assert;
import org.apache.lucene.search.Query;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * By overriding {@link org.hibernate.search.cfg.spi.SearchConfiguration#isIdProvidedImplicit()}
 * we allow to assume entities are annotated with ProvidedId.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class ImplicitProvidedIdTest {

	@Rule
	public ExpectedException exceptions = ExpectedException.none();

	@Test
	public void exceptionThrownWhenNotEnabled() {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Book.class ).indexed()
			//Entity missing both @DocumentId and @ProvidedId:
			.property( "title", ElementType.FIELD ).field()
			.property( "text", ElementType.FIELD ).field()
			;
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.setProgrammaticMapping( mapping )
			.addClass( Book.class );
		exceptions.expect( SearchException.class );
		exceptions.expectMessage( "HSEARCH000177" );
		storeBooksViaProvidedId( cfg, ProvidedId.defaultFieldName, false );
	}

	@Test
	public void usingConfigurationTypeOverride() {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Book.class ).indexed()
			//Entity missing both @DocumentId and @ProvidedId:
			.property( "title", ElementType.FIELD ).field()
			.property( "text", ElementType.FIELD ).field()
			;
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.setProgrammaticMapping( mapping )
			.setIdProvidedImplicit( true )
			.addClass( Book.class );
		storeBooksViaProvidedId( cfg, ProvidedId.defaultFieldName, false );
	}

	@Test
	public void usingProvidedIdAsOptionsOverride() {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Book.class ).indexed()
				.providedId().name( "myID" )
			//Entity missing both @DocumentId and @ProvidedId:
			.property( "title", ElementType.FIELD ).field()
			.property( "text", ElementType.FIELD ).field()
			;
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.setProgrammaticMapping( mapping )
			.setIdProvidedImplicit( true )
			.addClass( Book.class );
		storeBooksViaProvidedId( cfg, "myID", false );
	}

	@Test
	public void usingExplicitProvidedId() {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Book.class ).indexed()
				.providedId().name( "myID" )
			//Entity missing both @DocumentId and @ProvidedId:
			.property( "title", ElementType.FIELD ).field()
			.property( "text", ElementType.FIELD ).field()
			;
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.setProgrammaticMapping( mapping )
			.setIdProvidedImplicit( false ) //DEFAULT
			.addClass( Book.class );
		storeBooksViaProvidedId( cfg, "myID", false );
	}

	@Test
	public void usingDefaultSettings() {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Book.class ).indexed()
				.providedId().name( "myID" )
			//Entity missing both @DocumentId and @ProvidedId:
			.property( "title", ElementType.FIELD ).field()
			.property( "text", ElementType.FIELD ).field()
			;
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.setProgrammaticMapping( mapping )
			//.setIdProvidedImplicit( false ) //Test it's the default
			.addClass( Book.class );
		storeBooksViaProvidedId( cfg, "myID", false );
	}

	@Test
	public void documentIdNotOverriden() {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Book.class ).indexed()
				.property( "title", ElementType.FIELD ).documentId()
				.property( "text", ElementType.FIELD ).field()
			;
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
			.setProgrammaticMapping( mapping )
			//.setIdProvidedImplicit( false ) //Test it's the default
			.addClass( Book.class );
		storeBooksViaProvidedId( cfg, "title", true );
	}

	/**
	 * @param cfg The SearchFactory configuration to be tested
	 * @param fieldName The expected name of the ID field
	 */
	private void storeBooksViaProvidedId(SearchConfigurationForTest cfg, String fieldName, boolean matchTitle) {
		SearchIntegrator searchIntegrator = null;
		try {
			//Should fail right here when @ProvidedId is not enabled:
			searchIntegrator = new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();

			Book book = new Book();
			book.title = "Less is nice";
			book.text = "When using Infinispan Query, users have to always remember to add @ProvidedId on their classes" +
					" or a nasty exception will remind them. Can't we just assume it's always annotated?";
			String isbn = "some entity-external id";
			Work work = new Work( book, isbn, WorkType.ADD, false );
			TransactionContextForTest tc = new TransactionContextForTest();
			searchIntegrator.getWorker().performWork( work, tc );
			tc.end();

			QueryBuilder queryBuilder = searchIntegrator.buildQueryBuilder()
					.forEntity( Book.class )
					.get();

			Query query = queryBuilder.keyword()
				.onField( fieldName )
				.ignoreAnalyzer()
				.matching( matchTitle ? book.title : isbn )
				.createQuery();

			int queryResultSize = searchIntegrator.createHSQuery()
					.luceneQuery( query )
					.targetedEntities( Arrays.asList( new Class<?>[]{ Book.class } ) )
					.queryResultSize();
			Assert.assertEquals( 1, queryResultSize );
		}
		finally {
			if ( searchIntegrator != null ) {
				searchIntegrator.close();
			}
		}
	}

	/**
	 * Test entity. We use programmatic configuration to test different annotation combinations.
	 */
	static class Book {

		String title;

		String text;
	}

}
