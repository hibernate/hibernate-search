// $Id$
package org.hibernate.search.test.jgroups.slave;

import java.util.List;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import org.hibernate.search.backend.LuceneWork;

/**
 * @author Lukasz Moren
 */

public class JGroupsReceiver extends ReceiverAdapter {

	public static int queues;
	public static int works;

	public static void reset() {
		queues = 0;
		works = 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void receive(Message message) {

		List<LuceneWork> queue;
		try {
			queue = ( List<LuceneWork> ) message.getObject();
		}

		catch ( ClassCastException e ) {
			return;
		}
		queues++;
		works += queue.size();
	}
}
