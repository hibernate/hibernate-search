/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.setuputilities;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;

public class SearchIntegratorCreation {

	private SearchIntegratorCreation() {
		//do not construct
	}

	public static SearchIntegrator createIntegrator(ConnectionInfo connectionInfo, boolean refreshAfterWrite, String workerExecution) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();

		cfg.addProperty( "hibernate.search.default.indexmanager", "elasticsearch" );
		cfg.addProperty( "hibernate.search.default.elasticsearch.host", connectionInfo.getHost() );
		addIfNonNull( cfg, "hibernate.search.default.elasticsearch.username", connectionInfo.getUsername() );
		addIfNonNull( cfg, "hibernate.search.default.elasticsearch.password", connectionInfo.getPassword() );
		addIfNonNull( cfg, "hibernate.search.default.elasticsearch.aws.access_key", connectionInfo.getAwsAccessKey() );
		addIfNonNull( cfg, "hibernate.search.default.elasticsearch.aws.secret_key", connectionInfo.getAwsSecretKey() );
		addIfNonNull( cfg, "hibernate.search.default.elasticsearch.aws.region", connectionInfo.getAwsRegion() );
		cfg.addProperty( "hibernate.search.default.elasticsearch.required_index_status", "yellow" );
		cfg.addProperty( "hibernate.search.default.elasticsearch.index_schema_management_strategy", "drop-and-create-and-drop" );
		cfg.addProperty( "hibernate.search.default.elasticsearch.refresh_after_write", String.valueOf( refreshAfterWrite ) );
		cfg.addProperty( "hibernate.search.default.worker.execution", workerExecution );

		cfg.addClass( BookEntity.class );

		return new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
	}

	public static void preindexEntities(SearchIntegrator si, Dataset data, int numEntities) {
		println( "Starting index creation..." );
		Worker worker = si.getWorker();
		TransactionContextForTest tc = new TransactionContextForTest();
		boolean needsFlush = false;
		int i = 1;
		for ( ; i <= numEntities; i++ ) {
			BookEntity book = data.create( i );
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

	private static void addIfNonNull(SearchConfigurationForTest cfg, String propertyName, String propertyValue) {
		if ( propertyValue != null ) {
			cfg.addProperty( propertyName, propertyValue );
		}
	}

	private static void println(String string) {
		//We strictly disallow System.out usage in the whole project,
		//however this is a CLI tool so allow it here:
		//CHECKSTYLE:OFF
		System.out.println( string );
		//CHECKSTYLE:ON
	}

}
