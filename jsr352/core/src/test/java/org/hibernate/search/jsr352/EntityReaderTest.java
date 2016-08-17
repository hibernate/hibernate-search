/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;

import org.hibernate.ScrollableResults;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.internal.steps.lucene.EntityReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for item reader validation.
 *
 * @author Mincong Huang
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ContextHelper.class)
public class EntityReaderTest {

	private final Company[] COMPANIES = new Company[]{
			new Company( "Red Hat" ),
			new Company( "Google" ),
			new Company( "Microsoft" ) };

	@Mock
	private ScrollableResults scroll;

	@Mock
	private EntityManagerFactory emf;

	@Mock
	private PersistenceUnitUtil puUtil;

	@InjectMocks
	private EntityReader entityReader;

	@Before
	public void setUp() {
		Mockito.when( scroll.next() )
				.thenReturn( true )
				.thenReturn( true )
				.thenReturn( true )
				.thenReturn( false );
		Mockito.when( scroll.get( Mockito.anyInt() ) )
				.thenReturn( COMPANIES[0] )
				.thenReturn( COMPANIES[1] )
				.thenReturn( COMPANIES[2] );
		Mockito.when( emf.getPersistenceUnitUtil() ).thenReturn( puUtil );
		Mockito.when( puUtil.getIdentifier( Mockito.anyObject() ) ).thenReturn( "id" );
	}

	@Test
	public void testReadItem() throws Exception {

		for ( int i = 0; i < COMPANIES.length; i++ ) {
			Company realCompany = (Company) entityReader.readItem();
			assertEquals( COMPANIES[i], realCompany );
		}
		Object lastItem = entityReader.readItem();
		assertNull( lastItem );
	}
}
