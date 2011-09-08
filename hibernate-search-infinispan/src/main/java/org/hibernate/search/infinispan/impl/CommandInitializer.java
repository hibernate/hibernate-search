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

import org.hibernate.search.infinispan.impl.routing.CacheManagerMuxer;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ModuleCommandInitializer;

/**
 * With Infinispan custom RPCs are initially created by {@link CommandFactory},
 * which also restores the command specific parameters; there is then
 * a second initialization chance handled by a CommandInitializer.
 *
 * This is the Hibernate Search CommandInitializer, used to bind the command to
 * the services it will need to resolve the command.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CommandInitializer implements ModuleCommandInitializer {

	private CacheManagerMuxer muxer;

	@Override
	public void initializeReplicableCommand(ReplicableCommand command, boolean isRemote) {
		// we don't waste cycles to check it's the correct type, as that would be a
		// critical error anyway: let it throw a ClassCastException.
		CustomSearchCommand queryCommand = (CustomSearchCommand) command;
		queryCommand.fetchExecutionContext( this );
	}

	public void setMuxer(CacheManagerMuxer muxer) {
		this.muxer = muxer;
	}

	public CacheManagerMuxer getMuxer() {
		return this.muxer;
	}

}
