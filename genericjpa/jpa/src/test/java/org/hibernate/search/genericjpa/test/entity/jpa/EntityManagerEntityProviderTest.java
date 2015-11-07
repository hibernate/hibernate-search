/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.entity.jpa;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.genericjpa.db.events.triggers.MySQLTriggerSQLStringSource;
import org.hibernate.search.genericjpa.entity.impl.BasicEntityProvider;
import org.hibernate.search.genericjpa.test.db.events.jpa.DatabaseIntegrationTest;
import org.hibernate.search.genericjpa.test.db.events.jpa.MetaModelParser;
import org.hibernate.search.genericjpa.test.jpa.entities.Place;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Martin
 */
public class EntityManagerEntityProviderTest extends DatabaseIntegrationTest {

	@SuppressWarnings("unchecked")
	@Test
	public void test() throws SQLException, IOException {
		this.setup( "EclipseLink_MySQL", new MySQLTriggerSQLStringSource() );
		EntityManager em = this.emf.createEntityManager();
		MetaModelParser metaModelParser = new MetaModelParser();
		metaModelParser.parse( this.emf.getMetamodel() );
		BasicEntityProvider provider = new BasicEntityProvider( em, metaModelParser.getIdProperties() );
		try {

			assertEquals( "Valinor", ((Place) provider.get( Place.class, this.valinorId )).getName() );
			List<Place> batch = (List<Place>) provider.getBatch(
					Place.class, Arrays.asList(
							this.valinorId,
							this.helmsDeepId
					)
			);
			assertEquals( 2, batch.size() );
			// order is not preserved in getBatch!
			Set<String> names = batch.stream().map(
					(place) -> {
						return place.getName();
					}
			).collect( Collectors.toSet() );
			assertTrue( "didn't contain Valinor!", names.contains( "Valinor" ) );
			assertTrue( "didn't contain Helm's Deep", names.contains( "Helm's Deep" ) );
		}
		finally {
			if ( provider != null ) {
				provider.close();
			}
		}
	}

}
