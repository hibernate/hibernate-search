package org.hibernate.search.bridge.builtin;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.bridge.TwoWayStringBridge;

public class UUIDBridge implements TwoWayStringBridge {


	public String objectToString(Object object) {
		if(object == null) {
			return null;
		}
		return object.toString();
	}


	public Object stringToObject(String stringValue) {
		if(StringUtils.isEmpty(stringValue)) return null;
		return UUID.fromString(stringValue);
	}

}
