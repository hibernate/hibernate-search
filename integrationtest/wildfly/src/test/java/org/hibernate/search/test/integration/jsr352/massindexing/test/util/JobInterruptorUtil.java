/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jsr352.massindexing.test.util;


/**
 * Utils related to the Byteman rule "JobInterruptor.bm".
 *
 * @author Yoann Rodiere
 */
public class JobInterruptorUtil {

	private JobInterruptorUtil() {
	}

	public static void enable() {
		throw shouldHaveBeenInterrupted();
	}

	public static void disable() {
		throw shouldHaveBeenInterrupted();
	}

	private static IllegalStateException shouldHaveBeenInterrupted() {
		return new IllegalStateException( "A Byteman rule should have interrupted this call; maybe Byteman is not enabled?" );
	}

}
