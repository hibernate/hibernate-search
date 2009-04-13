//$Id$
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.util.StringHelper;


/**
 * Map an Enum field
 *
 * @author Sylvain Vieujot
 */
public class EnumBridge implements TwoWayStringBridge {

	private Class<? extends Enum> clazz = null;

    /**
     * @param clazz the class of the enum.
     */
    public EnumBridge(Class<? extends Enum> clazz) {
        this.clazz = clazz;
    }

	public Enum<? extends Enum> stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) return null;
		return Enum.valueOf( clazz, stringValue );
	}

	public String objectToString(Object object) {
		Enum e = (Enum) object;
		return e != null ? e.name() : null;
	}
}

