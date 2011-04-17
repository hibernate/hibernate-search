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

package org.hibernate.search.util;

import org.hibernate.search.SearchException;

/**
 * Common utility methods shared across subsystems dealing with JGroups
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class JGroupsHelper {
	
	/**
	 * Will throw an exception unless JVM property
	 * <code>java.net.preferIPv4Stack</code> was set to true.
	 * In some cases not defining it seems to hang state transfer
	 * without a meaningful error message. 
	 */
	public static void verifyIPv4IsPreferred() {
		boolean ipv4Preferred = Boolean.getBoolean( "java.net.preferIPv4Stack" );
		if ( ! ipv4Preferred ) {
			throw new SearchException( "Using JGroups requires the JVM property 'java.net.preferIPv4Stack' be set to true" );
		}
	}

}
