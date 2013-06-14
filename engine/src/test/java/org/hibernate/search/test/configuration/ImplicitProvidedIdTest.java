/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;
import java.util.Arrays;

import junit.framework.Assert;

import org.apache.lucene.search.Query;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.ManualTransactionContext;
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
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.default.directory_provider", "ram" )
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
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.default.directory_provider", "ram" )
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
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.default.directory_provider", "ram" )
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
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.default.directory_provider", "ram" )
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
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.default.directory_provider", "ram" )
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
		ManualConfiguration cfg = new ManualConfiguration()
			.addProperty( "hibernate.search.default.directory_provider", "ram" )
			.setProgrammaticMapping( mapping )
			//.setIdProvidedImplicit( false ) //Test it's the default
			.addClass( Book.class );
		storeBooksViaProvidedId( cfg, "title", true );
	}

	/**
	 * @param cfg The SearchFactory configuration to be tested
	 * @param fieldName The expected name of the ID field
	 */
	private void storeBooksViaProvidedId(ManualConfiguration cfg, String fieldName, boolean matchTitle) {
		SearchFactoryImplementor sf = null;
		try {
			//Should fail right here when @ProvidedId is not enabled:
			sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();

			Book book = new Book();
			book.title = "Less is nice";
			book.text = "When using Infinispan Query, users have to always remember to add @ProvidedId on their classes" +
					" or a nasty exception will remind them. Can't we just assume it's always annotated?";
			String isbn = "some entity-external id";
			Work work = new Work( book, isbn, WorkType.ADD, false );
			ManualTransactionContext tc = new ManualTransactionContext();
			sf.getWorker().performWork( work, tc );
			tc.end();

			QueryBuilder queryBuilder = sf.buildQueryBuilder()
					.forEntity( Book.class )
					.get();

			Query query = queryBuilder.keyword()
				.onField( fieldName )
				.ignoreAnalyzer()
				.matching( matchTitle ? book.title : isbn )
				.createQuery();

			int queryResultSize = sf.createHSQuery()
					.luceneQuery( query )
					.targetedEntities( Arrays.asList( new Class<?>[]{ Book.class } ) )
					.queryResultSize();
			Assert.assertEquals( 1, queryResultSize );
		}
		finally {
			if ( sf != null ) {
				sf.close();
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
