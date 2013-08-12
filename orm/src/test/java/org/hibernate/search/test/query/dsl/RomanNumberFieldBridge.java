/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.query.dsl;

import org.hibernate.search.bridge.StringBridge;

/**
 * Example of a StringBridge expecting to be applied on numeric objects.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class RomanNumberFieldBridge implements StringBridge {

	@Override
	public String objectToString(Object object) {
		if ( !( object instanceof Number ) ) {
			return null;
		}
		int v = ( (Number) object ).intValue();
		if ( v == 0 ) {
			return null;
		}
		if ( v == 1 ) {
			return "I";
		}
		if ( v == 2 ) {
			return "II";
		}
		if ( v == 3 ) {
			return "III";
		}
		if ( v == 4 ) {
			return "IV";
		}
		if ( v == 5 ) {
			return "IV";
		}
		// ... I bet someone has written a smarter converter
		return null;
	}

}
