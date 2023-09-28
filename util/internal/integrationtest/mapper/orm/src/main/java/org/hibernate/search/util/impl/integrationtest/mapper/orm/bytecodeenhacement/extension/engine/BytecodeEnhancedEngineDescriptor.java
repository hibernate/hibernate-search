/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.bytecodeenhacement.extension.engine;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.platform.engine.UniqueId;

public class BytecodeEnhancedEngineDescriptor extends JupiterEngineDescriptor {
	public BytecodeEnhancedEngineDescriptor(UniqueId uniqueId, JupiterConfiguration configuration) {
		super( uniqueId, configuration );
	}
}
