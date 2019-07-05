package nl.ijsberg;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.messages.Status;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 */
public class BoncodeTeamCityPlugin extends AgentLifeCycleAdapter implements AgentBuildRunner, AgentBuildRunnerInfo, Logger {

	BuildProgressLogger buildProgressLogger;

	@NotNull
	public BuildProcess createBuildProcess(@NotNull AgentRunningBuild agentRunningBuild, @NotNull BuildRunnerContext buildRunnerContext) throws RunBuildException {
		return new BoncodeTeamCityBuildProcess(buildRunnerContext, this);
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

	@Override
	public void buildStarted(AgentRunningBuild agentRunningBuild) {
		buildProgressLogger = agentRunningBuild.getBuildLogger();
		log(new LogEntry("build started"));
	}

	@Override
	public void buildFinished(@NotNull
							  AgentRunningBuild build,
							  @NotNull
							  BuildFinishedStatus buildStatus) {
		log(new LogEntry("build finished"));
		buildProgressLogger = null;
	}


	public void log(LogEntry entry) {
		Loggers.AGENT.info(entry.toString());
		if(buildProgressLogger != null) {
			buildProgressLogger.logMessage(new BuildMessage1("sId", "tId", Status.NORMAL, new Date(), entry));
		}
	}

    public void addAppender(Logger appender) {
	}

	public void removeAppender(Logger appender) {
	}


}
