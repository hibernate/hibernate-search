/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Same test as {@link AutomaticIndexingAssociationDeletionIT},
 * but with bytecode enhancement enabled, so that lazy one-to-one associations actually work.
 */
@TestForIssue(jiraKey = { "HSEARCH-3999", "HSEARCH-4303" })
@RunWith(BytecodeEnhancerRunner.class) // So that we can have lazy *ToOne associations
@EnhancementOptions(lazyLoading = true)
public class AutomaticIndexingAssociationDeletionBytecodeEnhancementIT
		extends AutomaticIndexingAssociationDeletionIT {

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		// Necessary for BytecodeEnhancerRunner, see BytecodeEnhancementIT.setup
		setupContext.withTcclLookupPrecedenceBefore();
	}

	@Test
	public void testBytecodeEnhancementWorked() {
		assertThat( AssociationNonOwner.class.getDeclaredMethods() )
				.extracting( Method::getName )
				.anyMatch( name -> name.startsWith( "$$_hibernate_" ) );
	}
}
