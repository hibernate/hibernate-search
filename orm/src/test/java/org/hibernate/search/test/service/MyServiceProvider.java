/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.service;

import java.util.Properties;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;

/**
 * @author Emmanuel Bernard
 */
public class MyServiceProvider implements ServiceProvider<MyService> {

	private static volatile boolean active = false;
	private static volatile boolean simulateCircularDependency = false;
	private MyService foo;

	@Override
	public void start(Properties properties, BuildContext context) {
		foo = new MyService();
		active = true;
		if ( simulateCircularDependency ) {
			context.getServiceManager().requestService( MyServiceProvider.class, context );
		}
	}

	@Override
	public MyService getService() {
		return foo;
	}

	@Override
	public void stop() {
		foo = null;
		active = false;
	}

	public static boolean isActive() { return active; }
	public static void resetActive() { active = false; }

	public static void setSimulateCircularDependency(boolean simulateCircularDependency) {
		MyServiceProvider.simulateCircularDependency = simulateCircularDependency;
	}

}
