/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.bridge;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 */
public class PropertiesExampleBridgeTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( DynamicIndexedValueHolder.class );

	@Test
	public void testPropertiesIndexing() {
		SearchFactoryImplementor searchFactory = sfHolder.getSearchFactory();
		Assert.assertNotNull( searchFactory.getIndexManagerHolder().getIndexManager( "all" ) );

		{
			// Store some test data:
			DynamicIndexedValueHolder holder = new DynamicIndexedValueHolder( "1" )
				.property( "age", "227" )
				.property( "name", "Thorin" )
				.property( "surname", "Oakenshield" )
				.property( "race", "dwarf" );

			Work work = new Work( holder, holder.id, WorkType.ADD, false );
			ManualTransactionContext tc = new ManualTransactionContext();
			searchFactory.getWorker().performWork( work, tc );
			tc.end();
		}

		QueryBuilder guestQueryBuilder = searchFactory.buildQueryBuilder().forEntity( DynamicIndexedValueHolder.class ).get();

		Query queryAllGuests = guestQueryBuilder.all().createQuery();

		List<EntityInfo> queryEntityInfos = searchFactory.createHSQuery().luceneQuery( queryAllGuests )
				.targetedEntities( Arrays.asList( new Class<?>[] { DynamicIndexedValueHolder.class } ) )
				.projection( "value.surname" )
				.queryEntityInfos();

		Assert.assertEquals( 1, queryEntityInfos.size() );
		EntityInfo entityInfo = queryEntityInfos.get( 0 );
		Assert.assertEquals( "Oakenshield", entityInfo.getProjection()[0] );

	}

}
