//$Id$
package org.hibernate.search.util;

import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public abstract class BinderHelper {

	private BinderHelper() {
	}

	/**
	 * Get attribute name out of member unless overriden by <code>name</code>
	 */
	public static String getAttributeName(XMember member, String name) {
		return StringHelper.isNotEmpty( name ) ?
				name :
				member.getName(); //explicit field name
	}
}
