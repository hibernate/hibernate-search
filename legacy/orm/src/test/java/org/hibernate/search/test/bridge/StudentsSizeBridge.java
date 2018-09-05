/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge;

import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * @author Emmanuel Bernard
 */
public class StudentsSizeBridge implements TwoWayStringBridge {

	@Override
	public Object stringToObject(String stringValue) {
		if ( null == stringValue || "".equals( stringValue ) ) {
			return 0;
		}
		return Integer.parseInt( stringValue );
	}

	@Override
	public String objectToString(Object object) {
		if ( object instanceof Teacher ) {
			Teacher teacher = (Teacher) object;
			if ( teacher.getStudents() != null && teacher.getStudents().size() > 0 ) {
				return String.valueOf( teacher.getStudents().size() );
			}
			else {
				return null;
			}
		}
		else {
			throw new IllegalArgumentException( StudentsSizeBridge.class + " used on a non-Teacher type: "
					+ object.getClass() );
		}
	}

}
