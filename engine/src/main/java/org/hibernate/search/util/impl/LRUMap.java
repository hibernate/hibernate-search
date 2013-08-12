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

package org.hibernate.search.util.impl;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Copied from Hibernate Core's org.hibernate.util.LRUMap
 * A simple LRU cache that implements the <code>Map</code> interface. Instances
 * are not thread-safe and should be synchronized externally, for instance by
 * using {@link java.util.Collections#synchronizedMap}.
 *
 * @author Manuel Dominguez Sarmiento
 */
public class LRUMap extends LinkedHashMap implements Serializable {

	private static final long serialVersionUID = -2613234214057068628L;

	private final int maxEntries;

	public LRUMap(int maxEntries) {
		super( maxEntries, .75f, true );
		this.maxEntries = maxEntries;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry eldest) {
		return ( size() > maxEntries );
	}
}
