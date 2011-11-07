/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend.impl.jms;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.impl.ContextHelper;
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
	 *  &nbsp; &nbsp;return session<br>
	 * }<br>
	 * </code>
	 * <p>
	 * eg in any container<br>
	 * <code>
	 * &#64;PersistenceContext private EntityManager entityManager;<br>
	 * <br>
	 * protected Session getSession() {<br>
	 *  &nbsp; &nbsp;return (Session) entityManager.getdelegate();<br>
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
			indexName = objectMessage.getStringProperty( JMSBackendQueueTask.INDEX_NAME_JMS_PROPERTY );
			indexManager = factory.getAllIndexesManager().getIndexManager( indexName );
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
			cleanSessionIfNeeded(session);
		}
	}

}
