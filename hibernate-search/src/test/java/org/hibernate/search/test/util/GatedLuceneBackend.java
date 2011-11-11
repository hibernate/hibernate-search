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
package org.hibernate.search.test.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.test.embedded.depth.LeakingLuceneBackend;

/**
 * A backend to use as test helper able to "switch off" the backend, to have it stop
 * processing operations. When the gate is closed, all operations are lost.
 *
 * Especially useful to test functionality while the index is updated asynchronously.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class GatedLuceneBackend extends LeakingLuceneBackend {

	public static final AtomicBoolean open = new AtomicBoolean( true );

	@Override
	public Runnable getProcessor(List<LuceneWork> queue) {
		if ( open.get() ) {
			return super.getProcessor( queue );
		}
		else {
			return new Runnable() {
				public void run() {
				}
			};
		}
	}

}
