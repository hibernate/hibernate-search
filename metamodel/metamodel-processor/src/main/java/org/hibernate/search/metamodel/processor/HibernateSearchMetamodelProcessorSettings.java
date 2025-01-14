/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor;

import java.util.Map;

public final class HibernateSearchMetamodelProcessorSettings {

	private HibernateSearchMetamodelProcessorSettings() {
	}

	private static final String PREFIX = "org.hibernate.search.metamodel.processor.";

	public static final String ADD_GENERATED_ANNOTATION = PREFIX + Radicals.ADD_GENERATED_ANNOTATION;

	public static class Radicals {

		private Radicals() {
		}

		public static final String ADD_GENERATED_ANNOTATION = "add_generated_annotation";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		public static final String ADD_GENERATED_ANNOTATION = Boolean.TRUE.toString();

		private Defaults() {
		}
	}

	public record Configuration(boolean addGeneratedAnnotation) {
		public Configuration(Map<String, String> options) {
			this( Boolean.parseBoolean( options.getOrDefault( ADD_GENERATED_ANNOTATION, Defaults.ADD_GENERATED_ANNOTATION ) ) );
		}
	}
}
