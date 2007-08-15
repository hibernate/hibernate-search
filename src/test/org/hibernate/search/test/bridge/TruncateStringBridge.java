//$Id$
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.ParameterizedBridge;

/**
 * @author Emmanuel Bernard
 */
public class TruncateStringBridge implements StringBridge, ParameterizedBridge {
    private int div;
    public Object stringToObject(String stringValue) {
        return stringValue;
    }

    public String objectToString(Object object) {
        String string = (String) object;
        return object != null ? string.substring( 0, string.length() / div ) : null;
    }

    public void setParameterValues(Map parameters) {
        div = Integer.valueOf( (String) parameters.get( "dividedBy" ) );
    }
}
