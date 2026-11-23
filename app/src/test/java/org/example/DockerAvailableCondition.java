package org.example;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * Disables Docker-based tests when no Docker daemon is reachable. On CI (the CI
 * environment variable is set, as on GitHub Actions) they always run, so a
 * missing Docker fails the build instead of silently skipping the tests.
 */
class DockerAvailableCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		if (System.getenv("CI") != null) {
			return ConditionEvaluationResult.enabled("always run on CI");
		}
		if (DockerClientFactory.instance().isDockerAvailable()) {
			return ConditionEvaluationResult.enabled("Docker is available");
		}
		return ConditionEvaluationResult.disabled("Docker is not available");
	}
}
