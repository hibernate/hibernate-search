/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.triggers.impl;

/**
 * Created by Martin on 14.11.2015.
 */
public class TriggerServiceConstants {

	private TriggerServiceConstants() {
		// can't touch this!
	}

	public static final String TRIGGER_BASED_BACKEND_KEY = "hibernate.search.enableTriggerBackend";
	public static final String TRIGGER_BASED_BACKEND_DEFAULT_VALUE = "false";

}
