/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for flushing a backend in async mode.
 *
 * @author gustavonalle
 */
public class AsyncBackendFlushTest {

	private static final int ENTITIES = 100;

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.worker.execution", "async" );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );
	private final IndexedTypeIdentifier testType = PojoIndexedTypeIdentifier.convertFromLegacy( Quote.class );

	@Test
	public void testFlush() throws Exception {
		writeData( 0, ENTITIES / 2 );
		flushIndex();
		assertDocumentsIndexed( ENTITIES / 2 );
		writeData( ENTITIES / 2, ENTITIES );
		flushIndex();
		assertDocumentsIndexed( ENTITIES );
	}

	private void flushIndex() {
		sfHolder.extractIndexManager( testType ).flushAndReleaseResources();
	}

	private void assertDocumentsIndexed(int number) {
		helper.assertThat().from( Quote.class ).hasResultSize( number );
	}

	private void writeData(int fromId, int toId) {
		for ( int i = fromId; i < toId; i++ ) {
			Quote quote = new Quote( i, Quote.class.getName() );
			helper.add( quote );
		}
	}

}
