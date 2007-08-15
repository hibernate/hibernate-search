//$Id$
package org.hibernate.search.test.jms.slave;

import java.util.List;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.JMSException;
import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;

import org.hibernate.search.backend.LuceneWork;

/**
 * @author Emmanuel Bernard
 */
@MessageDriven(activationConfig = {
      @ActivationConfigProperty(propertyName="destinationType", propertyValue="javax.jms.Queue"),
      @ActivationConfigProperty(propertyName="destination", propertyValue="queue/searchtest"),
      @ActivationConfigProperty(propertyName="DLQMaxResent", propertyValue="1")
   } )
public class SearchQueueChecker implements MessageListener {
	public static int queues;
	public static int works;

	public static void reset() {
		queues = 0;
		works = 0;
	}

	public void onMessage(Message message) {
		if (! (message instanceof ObjectMessage ) ) {
			return;
		}
		ObjectMessage objectMessage = (ObjectMessage) message;
		List<LuceneWork> queue;
		try {
			queue = (List<LuceneWork>) objectMessage.getObject();
		}
		catch (JMSException e) {
			return;
		}
		catch( ClassCastException e ) {
			return;
		}
		queues++;
		works+=queue.size();
	}
}
