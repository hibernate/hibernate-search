//$Id$
package org.hibernate.search.backend.impl.jms;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

import org.hibernate.HibernateException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;

/**
 * @author Emmanuel Bernard
 */
public class JMSBackendQueueProcessor implements Runnable {
	private List<LuceneWork> queue;
	private JMSBackendQueueProcessorFactory factory;

	public JMSBackendQueueProcessor(List<LuceneWork> queue,
									JMSBackendQueueProcessorFactory jmsBackendQueueProcessorFactory) {
		this.queue = queue;
		this.factory = jmsBackendQueueProcessorFactory;
	}

	public void run() {
		List<LuceneWork> filteredQueue = new ArrayList<LuceneWork>(queue);
		for (LuceneWork work : queue) {
			if ( work instanceof OptimizeLuceneWork ) {
				//we don't want optimization to be propagated
				filteredQueue.remove( work );
			}
		}
		if ( filteredQueue.size() == 0) return;
		factory.prepareJMSTools();
		QueueConnection cnn;
		QueueSender sender;
		QueueSession session;
		try {
			cnn = factory.getJMSFactory().createQueueConnection();
			//TODO make transacted parameterized
			session = cnn.createQueueSession( false, QueueSession.AUTO_ACKNOWLEDGE );

			ObjectMessage message = session.createObjectMessage();
			message.setObject( (Serializable) filteredQueue );

			sender = session.createSender( factory.getJmsQueue() );
			sender.send( message );

			session.close();
			cnn.close();
		}
		catch (JMSException e) {
			throw new HibernateException( "Unable to send Search work to JMS queue: " + factory.getJmsQueueName(), e );
		}
	}
}
