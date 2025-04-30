/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.writer.impl;

import java.util.Locale;

/**
 * Defines how a metamodel class name is constructed from the encountered class/property.
 */
public interface MetamodelNamesFormatter {
	MetamodelNamesFormatter DEFAULT = v -> String.format( Locale.ROOT, "%s__", v );

	String formatMetamodelClassName(String className);

	default String formatIndexFieldName(String metamodelClassName) {
		return "INDEX";
	}
}
