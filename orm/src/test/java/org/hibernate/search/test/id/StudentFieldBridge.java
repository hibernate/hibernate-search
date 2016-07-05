/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * This implementation is not a good example of a TwoWayStringBridge
 * as it makes several assumptions on the format of the strings
 * in the Department property.
 * Used only for a unit test, don't take it as a good example.
 */
public class StudentFieldBridge implements TwoWayStringBridge {

	@Override
	public Object stringToObject(String stringValue) {
		String[] split = stringValue.split( "_" );
		RegistrationId ret = new RegistrationId();
		ret.setDepartment( split[0] );
		ret.setStudentId( Integer.parseInt( split[1] ) );
		return ret;
	}

	@Override
	public String objectToString(Object object) {
		RegistrationId id = (RegistrationId) object;
		return id.getDepartment() + "_" + id.getStudentId();
	}

}
