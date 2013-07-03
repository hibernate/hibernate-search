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

package org.hibernate.search.test.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.junit.Assert.fail;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public class BytemanHelper extends Helper {
	public static final Log log = LoggerFactory.make();

	public static final AtomicInteger counter = new AtomicInteger();

	protected BytemanHelper(Rule rule) {
		super( rule );
	}

	public void sleepASecond() {
		try {
			log.info( "Byteman rule triggered: sleeping a second" );
			Thread.sleep( 1000 );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error( "unexpected interruption", e );
		}
	}

	public void assertBooleanValue(boolean actual, boolean expected) {
		if ( actual != expected ) {
			fail( "Unexpected boolean value" );
		}
	}

	public void countInvocation() {
		log.debug( "Increment call count" );
		counter.incrementAndGet();
	}

	public static int getAndResetInvocationCount() {
		return counter.getAndSet( 0 );
	}
}
