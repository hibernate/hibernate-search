// $Id$
package org.hibernate.search.backend.impl.jgroups;

import java.util.List;
import java.util.Properties;

import org.jgroups.Receiver;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Backend factory used in JGroups clustering mode in master node.
 * Wraps {@link LuceneBackendQueueProcessorFactory} with providing extra
 * functionality to receive Lucene works from slave nodes.
 *
 * @author Lukasz Moren
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory
 * @see org.hibernate.search.backend.impl.jgroups.SlaveJGroupsBackendQueueProcessorFactory
 */
public class MasterJGroupsBackendQueueProcessorFactory extends JGroupsBackendQueueProcessorFactory {

	private LuceneBackendQueueProcessorFactory luceneBackendQueueProcessorFactory;
	private Receiver masterListener;

	@Override
	public void initialize(Properties props, SearchFactoryImplementor searchFactory) {
		super.initialize( props, searchFactory );
		initLuceneBackendQueueProcessorFactory( props, searchFactory );

		registerMasterListener();
	}

	public Runnable getProcessor(List<LuceneWork> queue) {
		return luceneBackendQueueProcessorFactory.getProcessor( queue );
	}

	private void registerMasterListener() {
		//register JGroups receiver in master node to get Lucene docs from slave nodes
		masterListener = new JGroupsMasterMessageListener( searchFactory );
		channel.setReceiver( masterListener );
	}

	private void initLuceneBackendQueueProcessorFactory(Properties props, SearchFactoryImplementor searchFactory) {
		luceneBackendQueueProcessorFactory = new LuceneBackendQueueProcessorFactory();
		luceneBackendQueueProcessorFactory.initialize( props, searchFactory );
	}

	public Receiver getMasterListener() {
		return masterListener;
	}

	@Override
	public void close() {
		super.close();
		luceneBackendQueueProcessorFactory.close();
	}
}
