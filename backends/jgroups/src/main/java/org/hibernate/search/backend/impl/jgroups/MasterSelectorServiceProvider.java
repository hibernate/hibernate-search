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
package org.hibernate.search.backend.impl.jgroups;

import java.util.Properties;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;


/**
 * Provides the GlobalMasterSelector as an internal service; this is useful to guarantee
 * there is only one instance per SearchFactory exposed to all services
 * needing to interact with it.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class MasterSelectorServiceProvider implements ServiceProvider<NodeSelectorStrategyHolder> {

	private final NodeSelectorStrategyHolder selector = new NodeSelectorStrategyHolderImplementation();

	@Override
	public void start(Properties properties, BuildContext context) {
		// nothing to do
	}

	@Override
	public NodeSelectorStrategyHolder getService() {
		return selector;
	}

	@Override
	public void stop() {
		// nothing to do
	}

}
