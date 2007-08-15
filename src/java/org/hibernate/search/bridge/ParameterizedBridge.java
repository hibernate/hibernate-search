//$Id$
package org.hibernate.search.bridge;

import java.util.Map;

/**
 * Allow parameter injection to a given bridge
 *
 * @author Emmanuel Bernard
 */
public interface ParameterizedBridge {
	//TODO inject Properties? since the annotations cannot support Object attribute?
	void setParameterValues(Map parameters);
}
