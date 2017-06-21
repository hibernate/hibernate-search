/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Sanne Grinovero (C) 2013 Red Hat Inc.
 */
public class PropertiesExampleBridgeTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( DynamicIndexedValueHolder.class )
			// This property make sense only if you are using elasticsearch
			.withProperty( "hibernate.search.all.elasticsearch.dynamic_mapping", "true" );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void testPropertiesIndexing() {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		Assert.assertNotNull( searchFactory.getIndexManagerHolder().getIndexManager( "all" ) );

		// Store some test data:
		helper.add(
				new DynamicIndexedValueHolder( "1" )
				.property( "age", "227" )
				.property( "name", "Thorin" )
				.property( "surname", "Oakenshield" )
				.property( "race", "dwarf" )
		);

		helper.assertThat()
				.from( DynamicIndexedValueHolder.class )
				.projecting( "value.surname" )
				.matchesExactlySingleProjections( "Oakenshield" );
	}

}
