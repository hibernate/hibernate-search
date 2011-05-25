/* 
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

package org.hibernate.search.infinispan;

import java.util.UUID;

/**
 * Infinispan might cluster automatically with other tests being run at the same time
 * performing network autodiscovery, we make sure that won't happen isolating
 * each VM running tests by using a different UUID channel name. 
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class TestableJGroupsTransport extends org.infinispan.remoting.transport.jgroups.JGroupsTransport {
	
	/**
	 * Name of the JGroups channel used in test
	 */
	public static final String CHANNEL_NAME = UUID.randomUUID().toString();

	protected void startJGroupsChannelIfNeeded() {
		System.out.println( "Overriding configured JGroups channel name to " + CHANNEL_NAME );
		configuration.setClusterName( CHANNEL_NAME );
		super.startJGroupsChannelIfNeeded();
	}

}
