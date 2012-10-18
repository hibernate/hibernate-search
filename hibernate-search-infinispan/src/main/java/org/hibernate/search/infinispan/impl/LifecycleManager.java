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
package org.hibernate.search.infinispan.impl;

import org.hibernate.search.infinispan.impl.routing.CacheManagerMuxer;
import org.infinispan.config.Configuration;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.lifecycle.AbstractModuleLifecycle;

/**
 * Infinispan extension point: allows us to listen to Cache Lifecycle
 * by being discovered by Infinispan as an extending module.
 * This is an autodiscovered service listed in
 * <code>META-INF/services/org.infinispan.lifecycle.ModuleLifecycle</code>.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class LifecycleManager extends AbstractModuleLifecycle {

	/**
	 * Registers the CacheManagerMuxer in the cache registry before it gets started
	 * This isn't done after starting to make sure the Muxer is created only
	 * once per cache, regardless of how many
	 * Hibernate Search instances are using this CacheManger.
	 */
	@Override //Change this to cacheStarting(ComponentRegistry cr, String cacheName) when migrating to Infinispan 5.2
	public void cacheStarting(ComponentRegistry cr, Configuration cfg, String cacheName) {
		CacheManagerMuxer muxer = new CacheManagerMuxer();
		cr.registerComponent( muxer, CacheManagerMuxer.class );
	}

	@Override
	public void cacheStarted(ComponentRegistry cr, String cacheName) {
		CommandInitializer initializer = cr.getComponent( CommandInitializer.class );
		CacheManagerMuxer muxer = cr.getComponent( CacheManagerMuxer.class );
		initializer.setMuxer( muxer );
	}

}
