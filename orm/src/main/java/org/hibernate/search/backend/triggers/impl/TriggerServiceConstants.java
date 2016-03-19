/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.triggers.impl;

/**
 * Holds Constants related to the Trigger based backend
 *
 * @author Martin Braun
 */
public class TriggerServiceConstants {

	private TriggerServiceConstants() {
		// can't touch this!
	}

	//TODO: maybe create an IndexingMode enum value?
	//on the other side, IndexingMode.EVENT + Trigger and IndexingMode.MANUAL + Trigger
	//are both valid combinations as the Trigger backend can also be used
	//to retrieve updates that happened outside of Hibernate ORM (i.e. native SQL)
	//but users might also want to have updates from JPA immediately reflected
	//in their indexes (or not for the manual case)
	public static final String TRIGGER_BASED_BACKEND_KEY = "hibernate.search.enableTriggerBackend";
	public static final String TRIGGER_BASED_BACKEND_DEFAULT_VALUE = "false";

}
