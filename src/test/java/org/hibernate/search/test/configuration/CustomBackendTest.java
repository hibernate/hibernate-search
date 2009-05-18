// $Id$
package org.hibernate.search.test.configuration;

import junit.framework.TestCase;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;
import org.hibernate.search.impl.SearchFactoryImpl;
import org.hibernate.search.test.util.FullTextSessionBuilder;

/**
 * @author Sanne Grinovero
 */
public class CustomBackendTest extends TestCase {
	
	public void test() {
		verifyBackendUsage( "blackhole", BlackHoleBackendQueueProcessorFactory.class );
		verifyBackendUsage( "lucene", LuceneBackendQueueProcessorFactory.class );
		verifyBackendUsage( BlackHoleBackendQueueProcessorFactory.class );
		verifyBackendUsage( LuceneBackendQueueProcessorFactory.class );
	}
	
	private void verifyBackendUsage(String name, Class<? extends BackendQueueProcessorFactory> backendType) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		FullTextSession ftSession = builder
			.setProperty( "hibernate.search.worker.backend", name )
			.openFullTextSession();
		SearchFactoryImpl searchFactory = (SearchFactoryImpl) ftSession.getSearchFactory();
		ftSession.close();
		assertEquals( backendType, searchFactory.getBackendQueueProcessorFactory().getClass() );
		builder.close();
	}

	public void verifyBackendUsage(Class<? extends BackendQueueProcessorFactory> backendType) {
		verifyBackendUsage( backendType.getName(), backendType );
	}

}
