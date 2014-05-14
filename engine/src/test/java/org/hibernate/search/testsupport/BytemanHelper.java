/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.testsupport;

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
