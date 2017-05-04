/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.jgroups.logging.impl.Log;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;

/**
 * A {@link Receiver} that listens for messages from slave nodes and apply them.
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Ales Justin
 *
 */
public class JGroupsMasterMessageListener implements Receiver {

	private static final Log log = LoggerFactory.make( Log.class );

	private final SearchIntegrator integrator;
	private final NodeSelectorService selector;
	private final LuceneWorkSerializer luceneWorkSerializer;
	private volatile OperationDispatcher operationDispatcher;

	public JGroupsMasterMessageListener(BuildContext context, NodeSelectorService masterNodeSelector, LuceneWorkSerializer luceneWorkSerializer) {
		this.integrator = context.getUninitializedSearchIntegrator();
		this.selector = masterNodeSelector;
		this.luceneWorkSerializer = luceneWorkSerializer;
	}

	@Override
	public void receive(Message message) {
		final int offset = message.getOffset();
		final int bufferLength = message.getLength();
		final byte[] rawBuffer = message.getRawBuffer();
		try {
			byte[] serializedQueue = MessageSerializationHelper.extractSerializedQueue( offset, bufferLength, rawBuffer );
			List<LuceneWork> queue = luceneWorkSerializer.toLuceneWorks( serializedQueue );
			applyLuceneWorkLocally( queue, message );
		}
		catch (ClassCastException e) {
			log.illegalObjectRetrievedFromMessage( e );
		}
		catch (SearchException e) {
			log.illegalObjectRetrievedFromMessage( e );
		}
	}

	private void applyLuceneWorkLocally(List<LuceneWork> queue, Message message) {
		if ( queue != null && !queue.isEmpty() ) {
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"There are %d Lucene docs received from slave node %s to be processed if this node is the master",
						(Integer) queue.size(),
						message.getSrc()
				);
			}

			OperationDispatcher operationDispatcher = getOperationDispatcher();
			operationDispatcher.dispatch( queue, null );
		}
		else {
			log.receivedEmptyLuceneWorksInMessage();
		}
	}

	private OperationDispatcher getOperationDispatcher() {
		if ( operationDispatcher == null ) {
			operationDispatcher = integrator.createRemoteOperationDispatcher(
					indexManager -> selector.getMasterNodeSelector( indexManager.getIndexName() ).isIndexOwnerLocal()
			);
		}
		return operationDispatcher;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Implementations of JGroups interfaces
	// ------------------------------------------------------------------------------------------------------------------

	@Override
	public void viewAccepted(View view) {
		log.jGroupsReceivedNewClusterView( view );
		selector.viewAccepted( view );
	}

	@Override
	public void suspect(Address suspected_mbr) {
		//no-op
	}

	@Override
	public void block() {
		//no-op
	}

	@Override
	public void getState(OutputStream arg0) throws Exception {
	}

	@Override
	public void setState(InputStream arg0) throws Exception {
	}

	@Override
	public void unblock() {
	}
}
