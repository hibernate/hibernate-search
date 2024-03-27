/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.build.enforcer;

import static org.hibernate.search.build.enforcer.MavenProjectUtils.isAnyParentPublicParent;
import static org.hibernate.search.build.enforcer.MavenProjectUtils.isProjectDeploySkipped;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

@Named("publicModuleIsDeployedRule") // rule name - must start with lowercase character
public class PublicModuleIsDeployedRule extends AbstractEnforcerRule {


	// Inject needed Maven components
	@Inject
	private MavenSession session;

	public void execute() throws EnforcerRuleException {
		MavenProject currentProject = session.getCurrentProject();
		if ( isAnyParentPublicParent( currentProject ) && isProjectDeploySkipped( currentProject ) ) {
			throw new EnforcerRuleException(
					"Project " + currentProject.getArtifactId() + " is considered public but is *not* deployed;"
							+ " consider setting '" + MavenProjectUtils.DEPLOY_SKIP + "' to false in Maven properties." );
		}
	}


}
