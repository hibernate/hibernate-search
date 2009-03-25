//$Id$
package org.hibernate.search.backend;

import java.util.Map;
import java.util.Properties;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.impl.TransactionalWorker;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public abstract class WorkerFactory {

	private static Properties getProperties(SearchConfiguration cfg) {
		Properties props = cfg.getProperties();
		Properties workerProperties = new Properties();
		for (Map.Entry entry : props.entrySet()) {
			String key = (String) entry.getKey();
			if ( key.startsWith( Environment.WORKER_PREFIX ) ) {
				//key.substring( Environment.WORKER_PREFIX.length() )
				workerProperties.setProperty( key, (String) entry.getValue() );
			}
		}
		return workerProperties;
	}

	public static Worker createWorker(SearchConfiguration cfg, SearchFactoryImplementor searchFactoryImplementor) {
		Properties props = getProperties( cfg );
		String impl = props.getProperty( Environment.WORKER_SCOPE );
		Worker worker;
		if ( StringHelper.isEmpty( impl ) ) {
			worker = new TransactionalWorker();
		}
		else if ( "transaction".equalsIgnoreCase( impl ) ) {
			worker = new TransactionalWorker();
		}
		else {
			try {
				Class workerClass = ReflectHelper.classForName( impl, WorkerFactory.class );
				worker = (Worker) workerClass.newInstance();
			}
			catch (ClassNotFoundException e) {
				throw new SearchException( "Unable to find worker class: " + impl, e );
			}
			catch (IllegalAccessException e) {
				throw new SearchException( "Unable to instanciate worker class: " + impl, e );
			}
			catch (InstantiationException e) {
				throw new SearchException( "Unable to instanciate worker class: " + impl, e );
			}
		}
		worker.initialize( props, searchFactoryImplementor );
		return worker;
	}
}
