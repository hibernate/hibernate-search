/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.infinispan.impl.indexmanager;

import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.infinispan.impl.CommandInitializer;
import org.hibernate.search.infinispan.impl.CustomSearchCommand;
import org.hibernate.search.infinispan.impl.InfinispanCommandIds;
import org.hibernate.search.infinispan.impl.routing.CacheManagerMuxer;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.context.InvocationContext;

/**
 * Custom Infinispan RPC Command to transmit our specific backend operations
 * to other participating nodes in the cluster.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class IndexUpdateCommand extends BaseRpcCommand implements ReplicableCommand, CustomSearchCommand {

	private static final Log log = LoggerFactory.make( Log.class );

	public static final byte COMMAND_ID = InfinispanCommandIds.BACKEND_OPERATION;
	private byte[] message;
	private String indexName;

	/**
	 * Only used on receiver side as execution context:
	 */
	private CacheManagerMuxer muxer;

	public IndexUpdateCommand(String cacheName) {
		//Mandatory constructor as per Infinispan SPI
		super( cacheName );
	}

	@Override
	public byte getCommandId() {
		return COMMAND_ID;
	}

	@Override
	public Object perform(InvocationContext context) throws Throwable {
		IndexManager im = muxer.getActiveIndexManager( indexName );
		if ( im == null ) {
			// TODO send the message back / retry later?
			log.messageForUnknownIndexManager( indexName );
		}
		else {
			List<LuceneWork> luceneWorks = im.getSerializer().toLuceneWorks( this.message );
			im.performOperations( luceneWorks, null );
		}
		return null;
	}

	//Parameters are handled by the Infinispan RPC infrastructure
	@Override
	public void setParameters(int arg0, Object[] parameters) {
		this.message = (byte[]) parameters[0];
		this.indexName = (String) parameters[1];
	}

	//Make sure the array size and positions match #setParameters
	@Override
	public Object[] getParameters() {
		return new Object[] { message, indexName };
	}

	/**
	 * @param message Define the message payload: List of LuceneWork operations in serialized form.
	 */
	public void setMessage(byte[] message) {
		this.message = message;
	}

	/**
	 * @param indexName Specify the target index by naming it
	 */
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	//Infinispan SPI: RPC has not return value.
	@Override
	public boolean isReturnValueExpected() {
		return false;
	}

	//Custom SPI to inject dependencies on command receive, before processing.
	@Override
	public void fetchExecutionContext(CommandInitializer commandInitializer) {
		this.muxer = commandInitializer.getMuxer();
	}

}
