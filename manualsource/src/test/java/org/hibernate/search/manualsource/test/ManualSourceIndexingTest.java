/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.search.Query;
import org.junit.Test;

import org.hibernate.search.manualsource.WorkLoad;
import org.hibernate.search.manualsource.WorkLoadManager;
import org.hibernate.search.manualsource.impl.WorkLoadManagerImpl;
import org.hibernate.search.query.dsl.QueryBuilder;

import static org.fest.assertions.Assertions.assertThat;

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
				new MapBasedObjectInitializer(),
				properties
		);
		workLoadManager.close();
	}

	@Test
	public void testWorkLoadIndexingInBatch() throws Exception {
		WorkLoadManager workLoadManager = getWorkLoadManager();
		ConcurrentHashMap<Serializable, Object> database = getEntitySourceContextBuilder()
				.buildEntitySourceContextForWorkLoad()
				.unwrap( ConcurrentHashMap.class );

		Star sun = new Star();
		sun.setId( "1" );
		sun.setName( "Sun" );
		Star alphaCentauri = new Star();
		alphaCentauri.setId( "2" );
		alphaCentauri.setName( "Alpha Centauri" );

		// add the data to the database
		database.put( sun.getId(), sun );
		database.put( alphaCentauri.getId(), alphaCentauri );

		WorkLoad workLoad = workLoadManager.createWorkLoad();
		workLoad.startBatch();
		workLoad.index( sun );
		workLoad.index( alphaCentauri );
		workLoad.commitBatch();

		workLoad.startBatch();
		QueryBuilder qb = workLoad.getSearchFactory().buildQueryBuilder().forEntity( Star.class ).get();
		Query luceneQuery = qb.keyword().onField( "name" ).matching( "centauri" ).createQuery();
		assertThat( workLoad.createFullTextQuery( luceneQuery, Star.class ).list() ).onProperty( "name" ).containsExactly( alphaCentauri.getName() );
		workLoad.commitBatch();

		workLoad.startBatch();
		alphaCentauri.setId( "2" );
		alphaCentauri.setName( "Alpha Centauri" );
		workLoad.purge( Star.class, sun.getId() );
		workLoad.purge( Star.class, alphaCentauri.getId() );
		workLoad.commitBatch();

		workLoad.startBatch();
		luceneQuery = qb.all().createQuery();
		assertThat( workLoad.createFullTextQuery( luceneQuery, Star.class ).list() ).hasSize( 0 );
		workLoad.commitBatch();
	}

	@Test
	public void testWorkLoadIndexingOutOfBatch() throws Exception {
		WorkLoadManager workLoadManager = getWorkLoadManager();
		ConcurrentHashMap<Serializable, Object> database = getEntitySourceContextBuilder()
				.buildEntitySourceContextForWorkLoad()
				.unwrap( ConcurrentHashMap.class );

		Star sun = new Star();
		sun.setId( "1" );
		sun.setName( "Sun" );
		Star alphaCentauri = new Star();
		alphaCentauri.setId( "2" );
		alphaCentauri.setName( "Alpha Centauri" );

		// add the data to the database
		database.put( sun.getId(), sun );
		database.put( alphaCentauri.getId(), alphaCentauri );

		WorkLoad workLoad = workLoadManager.createWorkLoad();
		workLoad.index( sun );
		workLoad.index( alphaCentauri );

		QueryBuilder qb = workLoad.getSearchFactory().buildQueryBuilder().forEntity( Star.class ).get();
		Query luceneQuery = qb.keyword().onField( "name" ).matching( "centauri" ).createQuery();
		assertThat( workLoad.createFullTextQuery( luceneQuery, Star.class ).list() ).onProperty( "name" ).containsExactly( alphaCentauri.getName() );

		alphaCentauri.setId( "2" );
		alphaCentauri.setName( "Alpha Centauri" );
		workLoad.purge( Star.class, sun.getId() );
		workLoad.purge( Star.class, alphaCentauri.getId() );

		luceneQuery = qb.all().createQuery();
		assertThat( workLoad.createFullTextQuery( luceneQuery, Star.class ).list() ).hasSize( 0 );
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Star.class
		};
	}
}
