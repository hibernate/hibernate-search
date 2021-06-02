/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import java.util.function.BiConsumer;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexNode;

interface StubIndexSchemaFieldBuilder {

	StubIndexNode build(BiConsumer<String, StubIndexNode> fieldCollector);

}
