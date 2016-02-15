/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Created by Martin on 25.06.2015.
 */
public class IDFieldBridge implements TwoWayStringBridge {

	@Override
	public Object stringToObject(String stringValue) {
		//yeah, hacky, we only allow certain strings, but this is a testCustomUpdatedEntity, so who cares
		String[] split = stringValue.split( "_" );
		ID ret = new ID();
		ret.setFirstId( split[0] );
		ret.setSecondId( split[1] );
		return ret;
	}

	@Override
	public String objectToString(Object object) {
		ID id = (ID) object;
		return id.getFirstId() + "_" + id.getSecondId();
	}

}
