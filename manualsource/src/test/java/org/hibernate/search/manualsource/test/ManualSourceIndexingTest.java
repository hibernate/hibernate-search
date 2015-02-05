/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import org.hibernate.search.manualsource.WorkLoad;
import org.hibernate.search.manualsource.WorkLoadManager;
import org.hibernate.search.manualsource.impl.WorkLoadManagerImpl;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class ManualSourceIndexingTest extends ManualSourceTestBase {

	@Test
	public void testWorkLoadManagerBootstrap() throws Exception {
		List<Class<?>> classes = new ArrayList<>();
		classes.add( Star.class );
		Properties properties = new Properties();
		WorkLoadManager workLoadManager = new WorkLoadManagerImpl(
				classes,
				new MapBasedEntitySource(),
				new NameBasedIdExtractor( classes ),
				properties
		);
		workLoadManager.close();
	}

	@Test
	public void testWorkLoadIndexingInBatch() throws Exception {
		WorkLoadManager workLoadManager = getWorkLoadManager();

		WorkLoad workLoad = workLoadManager.createWorkLoad();
		workLoad.startBatch();
		Star sun = new Star();
		sun.setId( "1" );
		sun.setName( "Sun" );
		Star alphaCentauri = new Star();
		alphaCentauri.setId( "2" );
		alphaCentauri.setName( "Alpha Centauri" );
		workLoad.index( sun );
		workLoad.index( alphaCentauri );
		workLoad.commitBatch();
	}

	@Test
	public void testWorkLoadIndexingOutOfBatch() throws Exception {
		WorkLoadManager workLoadManager = getWorkLoadManager();

		WorkLoad workLoad = workLoadManager.createWorkLoad();
		Star sun = new Star();
		sun.setId( "1" );
		sun.setName( "Sun" );
		Star alphaCentauri = new Star();
		alphaCentauri.setId( "2" );
		alphaCentauri.setName( "Alpha Centauri" );
		workLoad.index( sun );
		workLoad.index( alphaCentauri );

	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Star.class
		};
	}
}
