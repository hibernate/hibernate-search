//$Id$
package org.hibernate.search.backend;

import java.util.List;
import java.util.ArrayList;

import org.hibernate.annotations.common.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */
public class WorkQueue {
	private List<Work> queue;

	private List<LuceneWork> sealedQueue;

	public WorkQueue(int size) {
		queue = new ArrayList<Work>(size);
	}

	private WorkQueue(List<Work> queue) {
		this.queue = queue;
	}

	public WorkQueue() {
		this(10);
	}


	public void add(Work work) {
		queue.add(work);
	}


	public List<Work> getQueue() {
		return queue;
	}

	public WorkQueue splitQueue() {
		WorkQueue subQueue = new WorkQueue( queue );
		this.queue = new ArrayList<Work>( queue.size() );
		return subQueue;
	}


	public List<LuceneWork> getSealedQueue() {
		if (sealedQueue == null) throw new AssertionFailure("Access a Sealed WorkQueue which has not been sealed");
		return sealedQueue;
	}

	public void setSealedQueue(List<LuceneWork> sealedQueue) {
		//invalidate the working queue for serializability
		queue = null;
		this.sealedQueue = sealedQueue;
	}

	public void clear() {
		queue.clear();
		if (sealedQueue != null) sealedQueue.clear();
	}

	public int size() {
		return queue.size();
	}
}
