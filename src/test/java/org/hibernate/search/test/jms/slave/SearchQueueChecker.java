//$Id$
package org.hibernate.search.test.jms.slave;

import java.util.List;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.JMSException;


import org.hibernate.search.backend.LuceneWork;

/**
 * Helper class to verify that the Slave places messages onto the queue.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class SearchQueueChecker implements MessageListener {
	public static int queues;
	public static int works;

	public static void reset() {
		queues = 0;
		works = 0;
	}

	@SuppressWarnings("unchecked")
	public void onMessage(Message message) {
		if ( !( message instanceof ObjectMessage ) ) {
			return;
		}
		ObjectMessage objectMessage = ( ObjectMessage ) message;

		List<LuceneWork> queue;
		try {
			queue = ( List<LuceneWork> ) objectMessage.getObject();
		}
		catch ( JMSException e ) {
			return;
		}
		catch ( ClassCastException e ) {
			return;
		}
		queues++;
		works += queue.size();
	}
}
