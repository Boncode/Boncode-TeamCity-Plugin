package nl.ijsberg;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.runner.BuildServiceAdapter;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import org.jetbrains.annotations.NotNull;

/**
 */
public class BoncodeTeamCityPlugin implements AgentBuildRunner, AgentBuildRunnerInfo {
	@NotNull
	public BuildProcess createBuildProcess(@NotNull AgentRunningBuild agentRunningBuild, @NotNull BuildRunnerContext buildRunnerContext) throws RunBuildException {
		return new BoncodeTeamCityBuildProcess(buildRunnerContext);  //To change body of implemented methods use File | Settings | File Templates.
	}

	@NotNull
	public AgentBuildRunnerInfo getRunnerInfo() {
		return this;
	}

	@NotNull
	public String getType() {
		return "Boncode";
	}

	public boolean canRun(@NotNull BuildAgentConfiguration buildAgentConfiguration) {
		return true;
	}

}
