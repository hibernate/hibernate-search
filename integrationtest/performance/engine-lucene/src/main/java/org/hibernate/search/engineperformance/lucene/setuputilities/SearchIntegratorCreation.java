/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.lucene.setuputilities;

import java.nio.file.Path;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engineperformance.lucene.model.BookEntity;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;

public class SearchIntegratorCreation {

	private SearchIntegratorCreation() {
		//do not construct
	}

	public static SearchIntegrator createIntegrator(String directorytype, String readerStrategy, Path storagePath) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		switch ( directorytype ) {
			case "local-heap" :
				cfg.addProperty( "hibernate.search.default.directory_provider", "local-heap" );
				break;
			case "fs" :
				cfg.addProperty( "hibernate.search.default.directory_provider", "filesystem" );
				break;
			case "fs-nrt" :
				cfg.addProperty( "hibernate.search.default.directory_provider", "filesystem" );
				cfg.addProperty( "hibernate.search.default.indexmanager", "near-real-time" );
				break;
			case "blackhole" :
				cfg.addProperty( "hibernate.search.default.worker.backend", "blackhole" );
				break;
			default :
				throw new RuntimeException( "Parameter 'directorytype'='" + directorytype + "' not recognized!" );
		}
		switch ( readerStrategy ) {
			case "async" :
				cfg.addProperty( "hibernate.search.default.reader.strategy", "async" );
				break;
			case "shared" :
				cfg.addProperty( "hibernate.search.default.reader.strategy", "shared" );
				break;
			default :
				throw new RuntimeException( "Parameter 'readerStrategy'='" + readerStrategy + "' not recognized!" );
		}
		cfg.addProperty( "hibernate.search.default.indexBase", storagePath.toString() );
		cfg.addClass( BookEntity.class );
		return new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
	}

	public static void preindexEntities(SearchIntegrator si, int numEntities) {
		println( "Starting index creation..." );
		Worker worker = si.getWorker();
		TransactionContextForTest tc = new TransactionContextForTest();
		boolean needsFlush = false;
		int i = 1;
		for ( ; i <= numEntities; i++ ) {
			BookEntity book = new BookEntity();
			book.setId( Long.valueOf( i ) );
			book.setText( "Some very long text should be stored here. No, I mean long as in a book." );
			book.setTitle( "Naaa" );
			book.setRating( Float.intBitsToFloat( i ) );
			Work work = new Work( book, book.getId(), WorkType.ADD, false );
			worker.performWork( work, tc );
			needsFlush = true;
			if ( i % 1000 == 0 ) {
				//commit in batches of 1000:
				tc.end();
				needsFlush = false;
				tc = new TransactionContextForTest();
			}
		}
		if ( needsFlush ) {
			//commit remaining work
			tc.end();
		}
		println( " ... created an index of " + numEntities + " entities." );
	}

	private static void println(String string) {
		//We strictly disallow System.out usage in the whole project,
		//however this is a CLI tool so allow it here:
		//CHECKSTYLE:OFF
		System.out.println( string );
		//CHECKSTYLE:ON
	}

}
