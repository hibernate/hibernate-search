/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.util.Properties;

import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.jgroups.logging.impl.Log;
import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class JGroupsBackend implements Backend {

	private static final Log log = LoggerFactory.make( Log.class );

	/**
	 * All configuration properties need to be prefixed with <blockquote>.jgroups
	 * </blockquote> to be interpreted by this backend.
	 */
	private static final String JGROUPS_CONFIGURATION_SPACE = "jgroups";

	/**
	 * Configuration property specific to the backend instance. When enabled
	 * the invoker thread will use JGroups in synchronous mode waiting for the
	 * ACK from the other parties; when disabled it is going to behave
	 * as fire and forget: delegates reliability to the JGroups configuration and returns
	 * immediately.
	 *
	 * The default value depends on the backend configuration: if it's set to async,
	 * then block_waiting_ack defaults to false.
	 *
	 * @see org.hibernate.search.cfg.Environment#WORKER_EXECUTION
	 */
	public static final String BLOCK_WAITING_ACK = "block_waiting_ack";

	/**
	 * This JGroups backend is meant to delegate to a different backend on the
	 * master node. Generally this is expected to be the Lucene backend,
	 * but this property allows to specify a different implementation for the delegate.
	 */
	public static final String DELEGATE_BACKEND = "delegate_backend";

	/**
	 * Specifies the timeout defined on messages sent to other nodes via the JGroups
	 * Channel. Value interpreted in milliseconds.
	 */
	public static final String MESSAGE_TIMEOUT_MS = "messages_timeout";

	/**
	 * Default value for the {@link #MESSAGE_TIMEOUT_MS} configuration property.
	 */
	public static final int DEFAULT_MESSAGE_TIMEOUT = 20000;

	private Properties properties;

	private ServiceManager serviceManager;

	private MessageSenderService messageSender;

	private LuceneWorkSerializer luceneWorkSerializer;

	private NodeSelectorService masterNodeSelector;

	private String delegateBackendName;

	private boolean sync;

	private boolean block;

	private long messageTimeout;

	@Override
	public void initialize(Properties properties, WorkerBuildContext context) {
		this.properties = properties;
		serviceManager = context.getServiceManager();
		/*
		 * This service in particular must be started eagerly, because it sets up
		 * the message consumers, allowing slaves to send messages to the master.
		 * Note that slaves may create index managers before the master,
		 * in particular when dynamic sharding is used.
		 */
		messageSender = serviceManager.requestService( MessageSenderService.class );
		luceneWorkSerializer = serviceManager.requestService( LuceneWorkSerializer.class );
		masterNodeSelector = serviceManager.requestService( NodeSelectorService.class );

		sync = BackendFactory.isConfiguredAsSync( properties );
		Properties jgroupsProperties = new MaskedProperty( properties, JGROUPS_CONFIGURATION_SPACE );
		block = ConfigurationParseHelper.getBooleanValue( jgroupsProperties, BLOCK_WAITING_ACK, sync );
		messageTimeout = ConfigurationParseHelper.getLongValue( jgroupsProperties, MESSAGE_TIMEOUT_MS, DEFAULT_MESSAGE_TIMEOUT );
		delegateBackendName = ConfigurationParseHelper.getString( jgroupsProperties, DELEGATE_BACKEND, "local" );
	}

	@Override
	public void close() {
		masterNodeSelector = null;
		serviceManager.releaseService( NodeSelectorService.class );
		luceneWorkSerializer = null;
		serviceManager.releaseService( LuceneWorkSerializer.class );
		messageSender = null;
		serviceManager.releaseService( MessageSenderService.class );
		serviceManager = null;
	}

	@Override
	public JGroupsBackendQueueProcessor createQueueProcessor(IndexManager indexManager, WorkerBuildContext context) {
		NodeSelectorStrategy selectionStrategy = createNodeSelectorStrategy( indexManager );

		String indexName = indexManager.getIndexName();
		assertLegacyOptionsNotUsed( properties, indexName );
		masterNodeSelector.setNodeSelectorStrategy( indexName, selectionStrategy );
		selectionStrategy.viewAccepted( messageSender.getView() ); // set current view?

		log.jgroupsBlockWaitingForAck( indexName, block );

		JGroupsBackendQueueTask jgroupsProcessor = new JGroupsBackendQueueTask(
				messageSender, indexManager, masterNodeSelector, luceneWorkSerializer,
				block, messageTimeout );

		JGroupsBackendQueueProcessor queueProcessor = new JGroupsBackendQueueProcessor(
				selectionStrategy, jgroupsProcessor,
				() -> createDelegateQueueProcessor( indexManager, context ) );
		return queueProcessor;
	}

	private BackendQueueProcessor createDelegateQueueProcessor(IndexManager indexManager, WorkerBuildContext context) {
		return BackendFactory.createBackend(delegateBackendName, indexManager, context, properties );
	}

	protected NodeSelectorStrategy createNodeSelectorStrategy(IndexManager indexManager) {
		return new AutoNodeSelector( indexManager.getIndexName() );
	}

	private static void assertLegacyOptionsNotUsed(Properties props, String indexName) {
		MaskedProperty jgroupsCfg = new MaskedProperty( props, "worker.backend.jgroups" );
		if ( jgroupsCfg.containsKey( "configurationFile" )
				|| jgroupsCfg.containsKey( "configurationXml" )
				|| jgroupsCfg.containsKey( "configurationString" )
				|| jgroupsCfg.containsKey( "clusterName" ) ) {
			throw log.legacyJGroupsConfigurationDefined( indexName );
		}
	}

}
