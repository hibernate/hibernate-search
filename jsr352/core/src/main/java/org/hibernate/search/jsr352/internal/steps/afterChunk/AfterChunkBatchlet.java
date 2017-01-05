/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.afterChunk;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.jboss.logging.Logger;

/**
 * Enhancements after the chunk step {@code produceLuceneDoc} (lucene document production)
 *
 * @author Mincong Huang
 */
@Named
public class AfterChunkBatchlet extends AbstractBatchlet {

	private static final Logger LOGGER = Logger.getLogger( AfterChunkBatchlet.class );

	@Inject
	@BatchProperty
	private String optimizeAtEnd;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	private Session session;

	public AfterChunkBatchlet() {
	}

	@Override
	public String process() throws Exception {

		if ( Boolean.parseBoolean( this.optimizeAtEnd ) ) {
			LOGGER.info( "optimizing all entities ..." );
			session = emf.unwrap( SessionFactory.class ).openSession();
			ContextHelper.getSearchintegrator( session ).optimize();
		}
		return null;
	}

	@Override
	public void stop() throws Exception {
		try {
			session.close();
		}
		catch (Exception e) {
			LOGGER.error( e );
		}
	}
}
