/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.provider;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.junit.Rule;
import org.junit.Test;

public class ConflictingBridgeDefinitionTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void testMultipleMatchingFieldBridges() throws Exception {
		SearchConfiguration conf = new HibernateManualConfiguration()
				.addClass( Theater.class )
				.addClass( Chain.class );
		boolean throwException = false;
		try {
			integratorResource.create( conf );
		}
		catch (SearchException e) {
			assertThat( e.getMessage() ).contains( "TheaterBridgeProvider1" );
			throwException = true;
		}
		assertThat( throwException ).isTrue();
	}

}
