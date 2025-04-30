/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.writer.impl;

import java.util.Locale;

record ClassProperty(String type, String name) implements Comparable<ClassProperty> {

	public String asParameter() {
		return type + " " + name;
	}

	public String asSetInConstructor() {
		return "this." + name + " = " + name;
	}

	public String asGetter() {
		return String.format( Locale.ROOT, "public %s %s() { return this.%s; }", type, name, name );
	}

	@Override
	public int compareTo(ClassProperty o) {
		return name.compareTo( o.name );
	}
}
