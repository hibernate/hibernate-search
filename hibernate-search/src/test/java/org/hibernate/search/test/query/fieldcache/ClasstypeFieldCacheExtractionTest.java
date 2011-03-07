/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.search.test.query.fieldcache;

import java.lang.annotation.ElementType;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.FieldCacheType;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.configuration.Address;
import org.hibernate.search.test.configuration.Country;
import org.hibernate.search.test.configuration.Item;
import org.hibernate.search.test.configuration.ProductCatalog;
import org.hibernate.search.test.util.FieldSelectorLeakingReaderProvider;
import org.hibernate.search.test.util.FullTextSessionBuilder;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ClasstypeFieldCacheExtractionTest extends TestCase {
	
	public void testWithoutFieldCacheOnMixedIds() {
		// no caches, mixed classes and multiple id fieldnames
		wrapper( FieldCacheType.NO, true, "id", "addressId", ProjectionConstants.OBJECT_CLASS );
	}
	
	public void testWithFieldCacheOnTypOnMixedIds() {
		// multiple id fieldnames, multiple classes but cached
		wrapper( FieldCacheType.CLASS, true, "id", "addressId" );
	}
	
	public void testWithFieldCacheOnTypeAndIdOnMixedIds() {
		// works the same as TYPE because one of the entities uses a different fieldname
		// which forces us to disable ID caching
		wrapper( FieldCacheType.CLASS_AND_ID, true, "id", "addressId" );
	}
	
	public void testWithoutFieldCache() {
		// single type: doesn't need classtype even without caches
		wrapper( FieldCacheType.NO, false, "id" );
	}
	
	public void testWithFieldCacheOnType() {
		// single type: doesn't need classtype cache
		wrapper( FieldCacheType.CLASS, false, "id" );
	}
	
	public void testWithFieldCacheOnTypeAndId() {
		// nothing needs to be extracted, full cache
		wrapper( FieldCacheType.CLASS_AND_ID, false );
	}
	
	public void wrapper(FieldCacheType usingFieldCache, boolean usingMixedIds, String... expectedLoadedFields) {
		Mapping.enableFieldCache = usingFieldCache;
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		if ( usingMixedIds ) {
			builder
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Country.class );
		}
		builder.addAnnotatedClass( Item.class )
			.addAnnotatedClass( ProductCatalog.class )
			.setProperty( Environment.READER_STRATEGY, org.hibernate.search.test.util.FieldSelectorLeakingReaderProvider.class.getName() )
			.setProperty( Environment.MODEL_MAPPING, Mapping.class.getName() )
			.build();
		try {
			storeDemoData( builder );
			performtest( builder, expectedLoadedFields );
		}
		finally {
			builder.close();
		}
	}
	
	private void performtest(FullTextSessionBuilder builder, String... expectedLoadedFields) {
		QueryBuilder queryBuilder = builder.getSearchFactory().buildQueryBuilder().forEntity( Item.class ).get();
		Query query = queryBuilder.all().createQuery();
		FullTextSession fullTextSession = builder.openFullTextSession();
		fullTextSession.beginTransaction();
		FieldSelectorLeakingReaderProvider.resetFieldSelector();
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query );
		List list = fullTextQuery.list();
		Assert.assertEquals( 2, list.size() );
		FieldSelectorLeakingReaderProvider.assertFieldSelectorEnabled( expectedLoadedFields );
	}

	private void storeDemoData(FullTextSessionBuilder builder) {
		FullTextSession fullTextSession = builder.openFullTextSession();
		Transaction transaction = fullTextSession.beginTransaction();
		Item ssd = new Item();
		ssd.setDescription( "intel solid state disk" );
		Item wd = new Item();
		wd.setDescription( "western digital hibrid disk" );
		fullTextSession.persist( ssd );
		fullTextSession.persist( wd );
		transaction.commit();
		fullTextSession.close();
	}

	// trick to perform the same test on three different configurations:
	public static class Mapping {
		
		static FieldCacheType enableFieldCache;

		@Factory
		public SearchMapping build() {
			SearchMapping mapping = new SearchMapping();
			mapping
					.entity( Address.class )
						.indexed()
						.cacheFromIndex( enableFieldCache )
						.indexName( "single-index" )
					.entity( Country.class )
						.indexed()
						.cacheFromIndex( enableFieldCache )
						.indexName( "single-index" )
						.property( "name", ElementType.FIELD )
					.entity( Item.class )
						.indexed()
						.cacheFromIndex( enableFieldCache );
			return mapping;
		}
		
	}
	
}
