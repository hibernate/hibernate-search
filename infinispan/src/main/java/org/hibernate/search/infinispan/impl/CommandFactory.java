/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.infinispan.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.infinispan.impl.indexmanager.IndexUpdateCommand;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ExtendedModuleCommandFactory;
import org.infinispan.commands.remote.CacheRpcCommand;

/**
 * Responsible for matching Command IDs to Command instances
 * for reception of remote RPCs via Infinispan.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CommandFactory implements ExtendedModuleCommandFactory {

	private static final Log log = LoggerFactory.make( Log.class );

	@Override
	public CacheRpcCommand fromStream(byte commandId, Object[] args, String cacheName) {
		final CacheRpcCommand c;
		switch ( commandId ) {
		case IndexUpdateCommand.COMMAND_ID:
			c = new IndexUpdateCommand( cacheName );
			break;
		default:
			throw log.unknownInfinispanCommandId( commandId );
		}
		c.setParameters( commandId, args );
		return c;
	}

	@Override
	public ReplicableCommand fromStream(byte commandId, Object[] args) {
		// Should not be called as this factory only
		// provides cache specific replicable commands.
		return null;
	}

	@Override
	public Map<Byte, Class<? extends ReplicableCommand>> getModuleCommands() {
		Map<Byte, Class<? extends ReplicableCommand>> map = new HashMap<Byte, Class<? extends ReplicableCommand>>( 1 );
		map.put( IndexUpdateCommand.COMMAND_ID, IndexUpdateCommand.class );
		return map;
	}

}
