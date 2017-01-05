/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

import org.hibernate.Session;

/**
 * Data model for each partition of step {@code produceLuceneDoc}. It contains a partition-level indexing progress and
 * the session attached to this partition. Notice that the batch runtime maintains one clone per partition and each
 * partition is running on a single thread. Therefore, session is not shared with other threads / partitions.
 * 
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class PartitionContextData implements Serializable {

	private static final long serialVersionUID = 1961574468720628080L;

	private PartitionProgress partitionProgress;

	/**
	 * Hibernate session, unwrapped from EntityManager. It is stored for sharing the session between item reader and
	 * item processor. Notice that item reader and item processor of the same partition always run in the same thread,
	 * so it should be OK. When the job stops, session object will be released before persisting this class's instance.
	 */
	private Session session;

	public PartitionContextData(int partitionId, String entityName) {
		partitionProgress = new PartitionProgress( partitionId, entityName );
	}

	public void documentAdded(int increment) {
		partitionProgress.documentsAdded( increment );
	}

	public PartitionProgress getPartitionProgress() {
		return partitionProgress;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}
}
