/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Bridge an {@link Enum} to a {@link String} using  {@link Enum#name()}.
 *
 * @author Sylvain Vieujot
 */
public class EnumBridge implements TwoWayStringBridge, AppliedOnTypeAwareBridge {

	private Class<? extends Enum> clazz = null;

	@Override
	public Enum<? extends Enum> stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		return Enum.valueOf( clazz, stringValue );
	}

	@Override
	public String objectToString(Object object) {
		Enum e = (Enum) object;
		return e != null ? e.name() : null;
	}

	@Override
	public void setAppliedOnType(Class<?> returnType) {
		@SuppressWarnings("unchecked") //only called for an enum
		Class<? extends Enum> enumReturnType = (Class<? extends Enum>) returnType;
		this.clazz = enumReturnType;
	}
}
