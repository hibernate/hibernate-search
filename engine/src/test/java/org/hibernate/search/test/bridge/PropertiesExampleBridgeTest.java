/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
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
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		Assert.assertNotNull( searchFactory.getIndexManagerHolder().getIndexManager( "all" ) );

		{
			// Store some test data:
			DynamicIndexedValueHolder holder = new DynamicIndexedValueHolder( "1" )
				.property( "age", "227" )
				.property( "name", "Thorin" )
				.property( "surname", "Oakenshield" )
				.property( "race", "dwarf" );

			Work work = new Work( holder, holder.id, WorkType.ADD, false );
			TransactionContextForTest tc = new TransactionContextForTest();
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
