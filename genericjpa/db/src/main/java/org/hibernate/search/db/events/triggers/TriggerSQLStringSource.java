/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.events.triggers;

import org.hibernate.search.db.events.impl.EventModelInfo;
import org.hibernate.search.db.EventType;

/**
 * Classes that implement this interface provide means to create the Triggers needed on the database to write C_UD
 * information about entities in the index into the specific Updates-Table.
 *
 * The order of execution during creation is:
 * <ol>
 *     <li>{@link #getSetupCode()}</li>
 *     <li>{@link #getUpdateTableCreationCode(EventModelInfo)}</li>
 *     <li>{@link #getSpecificSetupCode(EventModelInfo)}</li>
 *     <li>{@link #getTriggerCreationCode(EventModelInfo, int)}</li>
 * </ol>
 *
 * The order during deletion is:
 * <ol>
 *     <li>{@link #getTriggerDropCode(EventModelInfo, int)}</li>
 *     <li>{@link #getSpecificUnSetupCode(EventModelInfo)}</li>
 *     <li>{@link #getUpdateTableDropCode(EventModelInfo)}</li>
 *     <li>{@link #getUnSetupCode()}</li>
 * </ol>
 *
 * @author Martin Braun
 */
public interface TriggerSQLStringSource {

	/**
	 * this undoes the changes from {@link #getSetupCode()}
	 */
	String[] getUnSetupCode();

	/**
	 * this is executed first
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

	/**
	 * the code to create the Update tables with
	 *
	 * @param eventModelInfo the EventModelInfo/columnTypes this corresponds to
	 */
	String[] getUpdateTableCreationCode(EventModelInfo eventModelInfo);

	/**
	 * the code to drop the Update tables with
	 *
	 * @param eventModelInfo the EventModelInfo/columnTypes this corresponds to
	 */
	String[] getUpdateTableDropCode(EventModelInfo eventModelInfo);

	/**
	 * @return the token to delimit identifiers with in this database
	 */
	String getDelimitedIdentifierToken();

}
