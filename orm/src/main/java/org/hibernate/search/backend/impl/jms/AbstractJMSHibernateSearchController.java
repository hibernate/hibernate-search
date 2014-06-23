/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.jms;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.Session;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Implement the Hibernate Search controller responsible for processing the
 * work send through JMS by the slave nodes.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public abstract class AbstractJMSHibernateSearchController implements MessageListener {

	private static final Log log = LoggerFactory.make();

	/**
	 * Return the current or give a new session
	 * This session is not used per se, but is the link to access the Search configuration.
	 * <p>
	 * A typical EJB 3.0 usecase would be to get the session from the container (injected)
	 * eg in JBoss EJB 3.0
	 * <p>
	 * <code>
	 * &#64;PersistenceContext private Session session;<br>
	 * <br>
	 * protected Session getSession() {<br>
	 * &nbsp; &nbsp;return session<br>
	 * }<br>
	 * </code>
	 * <p>
	 * eg in any container<br>
	 * <code>
	 * &#64;PersistenceContext private EntityManager entityManager;<br>
	 * <br>
	 * protected Session getSession() {<br>
	 * &nbsp; &nbsp;return (Session) entityManager.getdelegate();<br>
	 * }<br>
	 * </code>
	 */
	protected abstract Session getSession();

	/**
	 * Ensure to clean the resources after use.
	 * If the session has been directly or indirectly injected, this method is empty
	 */
	protected abstract void cleanSessionIfNeeded(Session session);

	/**
	 * Process the Hibernate Search work queues received
	 */
	@Override
	public void onMessage(Message message) {
		if ( !( message instanceof ObjectMessage ) ) {
			log.incorrectMessageType( message.getClass() );
			return;
		}
		final ObjectMessage objectMessage = (ObjectMessage) message;
		final String indexName;
		final List<LuceneWork> queue;
		final IndexManager indexManager;
		Session session = getSession();
		SearchFactoryImplementor factory = ContextHelper.getSearchFactory( session );
		try {
			indexName = objectMessage.getStringProperty( Environment.INDEX_NAME_JMS_PROPERTY );
			indexManager = factory.getIndexManagerHolder().getIndexManager( indexName );
			if ( indexManager == null ) {
				log.messageReceivedForUndefinedIndex( indexName );
				return;
			}
			queue = indexManager.getSerializer().toLuceneWorks( (byte[]) objectMessage.getObject() );
			indexManager.performOperations( queue, null );
		}
		catch (JMSException e) {
			log.unableToRetrieveObjectFromMessage( message.getClass(), e );
			return;
		}
		catch (ClassCastException e) {
			log.illegalObjectRetrievedFromMessage( e );
			return;
		}
		finally {
			cleanSessionIfNeeded( session );
		}
	}

}
