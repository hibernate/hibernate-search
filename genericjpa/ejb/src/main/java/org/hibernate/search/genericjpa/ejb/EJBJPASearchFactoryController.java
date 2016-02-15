/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.genericjpa.JPASearchFactoryController;
import org.hibernate.search.genericjpa.Setup;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer;
import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.jpa.FullTextEntityManager;

@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class EJBJPASearchFactoryController implements JPASearchFactoryController {

	private static final String PROPERTIES_PATH = "/META-INF/hsearch.properties";

	@PersistenceUnit
	private EntityManagerFactory emf;

	private JPASearchFactoryController jpaSearchFactoryController;

	@PostConstruct
	public void start() {
		Properties properties = new Properties();
		try (InputStream is = EJBJPASearchFactoryController.class.getResource( PROPERTIES_PATH ).openStream()) {
			properties.load( is );
		}
		catch (NullPointerException | IOException e) {
			throw new SearchException(
					"couldn't load hibernate-search specific properties from: " + PROPERTIES_PATH,
					e
			);
		}
		this.jpaSearchFactoryController = Setup.createSearchFactoryController( this.emf, properties );
	}

	@PreDestroy
	public void stop() {
		try {
			if ( this.jpaSearchFactoryController != null ) {
				this.jpaSearchFactoryController.close();
			}
		}
		catch (Exception e) {
			throw new SearchException( e );
		}
	}

	public void addUpdateConsumer(UpdateConsumer updateConsumer) {
		if ( this.jpaSearchFactoryController != null ) {
			this.jpaSearchFactoryController.addUpdateConsumer( updateConsumer );
		}
	}

	public void removeUpdateConsumer(UpdateConsumer updateConsumer) {
		if ( this.jpaSearchFactoryController != null ) {
			this.jpaSearchFactoryController.removeUpdateConsumer( updateConsumer );
		}
	}

	@Override
	public void close() {
		throw new SearchException( "A Container Managed SearchFactoryController cannot be closed" );
	}

	@Override
	public SearchFactory getSearchFactory() {
		if ( this.jpaSearchFactoryController != null ) {
			return this.jpaSearchFactoryController.getSearchFactory();
		}
		return null;
	}

	@Override
	public void pauseUpdating(boolean pause) {
		if ( this.jpaSearchFactoryController != null ) {
			this.jpaSearchFactoryController.pauseUpdating( pause );
		}
	}

	@Override
	public FullTextEntityManager getFullTextEntityManager(EntityManager em) {
		if ( this.jpaSearchFactoryController != null ) {
			return this.jpaSearchFactoryController.getFullTextEntityManager( em );
		}
		return null;
	}

}
