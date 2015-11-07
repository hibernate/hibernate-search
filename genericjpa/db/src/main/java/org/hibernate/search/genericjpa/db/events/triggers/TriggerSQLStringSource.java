/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.triggers;

import org.hibernate.search.genericjpa.db.events.impl.EventModelInfo;
import org.hibernate.search.genericjpa.db.EventType;

/**
 * Classes that implement this interface provide means to create the Triggers needed on the database to write C_UD
 * information about entities in the index into the specific Updates-Table.
 *
 * @author Martin Braun
 */
public interface TriggerSQLStringSource {

	String[] getUnSetupCode();

	/**
	 * this is executed first, this can not be undone
	 */
	String[] getSetupCode();

	/**
	 * this has to be executed before every call to getTriggerCreationCode
	 *
	 * @param eventModelInfo the EventModelInfo/columnTypes this corresponds to
	 */
	String[] getSpecificSetupCode(EventModelInfo eventModelInfo);

	/**
	 * this removes all changes made by {@link #getSpecificSetupCode(EventModelInfo)}
	 *
	 * @param eventModelInfo the EventModelInfo/columnTypes this corresponds to
	 */
	String[] getSpecificUnSetupCode(EventModelInfo eventModelInfo);

	/**
	 * this creates a specific trigger
	 *
	 * @param eventModelInfo the EventModelInfo/columnTypes this corresponds to
	 * @param eventType see {@link EventType}
	 */
	String[] getTriggerCreationCode(EventModelInfo eventModelInfo, int eventType);

	/**
	 * this removes a specific trigger created by {@link #getTriggerCreationCode(EventModelInfo, int)}
	 *
	 * @param eventModelInfo the EventModelInfo/columnTypes this corresponds to
	 * @param eventType see {@link EventType}
	 */
	String[] getTriggerDropCode(EventModelInfo eventModelInfo, int eventType);

	String[] getUpdateTableCreationCode(EventModelInfo info);

	String[] getUpdateTableDropCode(EventModelInfo info);

	String getDelimitedIdentifierToken();

}
