/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Names of configuration scope namespaces used to construct a {@link ConfigurationScope}.
 *
 * @see ConfigurationScope
 * @see	ConfigurationProvider
 */
@Incubating
public final class ConfigurationScopeNamespaces {
	private ConfigurationScopeNamespaces() {
	}

	public static final String GLOBAL = "global";
	public static final String BACKEND = "backend";
	public static final String INDEX = "index";
}
