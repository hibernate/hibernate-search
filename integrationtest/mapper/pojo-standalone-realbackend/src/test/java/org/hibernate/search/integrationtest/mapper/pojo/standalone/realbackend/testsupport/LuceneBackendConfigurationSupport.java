/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport;


import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

public final class LuceneBackendConfigurationSupport {

	private LuceneBackendConfigurationSupport() {
	}

	public static BackendConfiguration simple() {
		return new LuceneBackendConfiguration();
	}


}
