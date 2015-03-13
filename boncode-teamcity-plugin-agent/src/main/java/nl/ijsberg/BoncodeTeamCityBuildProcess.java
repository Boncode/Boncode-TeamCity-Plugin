package nl.ijsberg;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.log.Loggers;
import nl.ijsberg.analysis.server.buildserver.BuildServerToMonitorLink;
import org.apache.log4j.Logger;
import org.ijsberg.iglu.logging.LogEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 */
public class BoncodeTeamCityBuildProcess implements BuildProcess, org.ijsberg.iglu.logging.Logger {

	private BuildRunnerContext context;

	private static final Logger logger = Logger.getLogger(BoncodeTeamCityBuildProcess.class);

	private String monitorUploadDirectory;
	private String analysisProperties;
	private String sourceRoot;
	private String checkoutDir;



	public BoncodeTeamCityBuildProcess(BuildRunnerContext context) {
		this.context = context;
		Map<String, String> configParameters = context.getConfigParameters();
		Map<String, String> buildParameters = context.getConfigParameters();
		Loggers.AGENT.info("config parameters: " + configParameters);
		Loggers.AGENT.info("build parameters: " + buildParameters);
		Loggers.AGENT.info("Checkout dir: " + context.getBuild().getCheckoutDirectory());

		checkoutDir = context.getBuild().getCheckoutDirectory().getAbsolutePath();
		sourceRoot = checkoutDir + "/" + buildParameters.get(BuildServerToMonitorLink.SOURCE_ROOT);
		monitorUploadDirectory = buildParameters.get(BuildServerToMonitorLink.MONITOR_UPLOAD_DIRECTORY);
		analysisProperties = buildParameters.get(BuildServerToMonitorLink.ANALYSIS_PROPERTIES_FILENAME);

		BuildServerToMonitorLink.throwIfPropertiesNotOk(
				analysisProperties,
				monitorUploadDirectory,
				sourceRoot);
	}

	public void start() throws RunBuildException {
		Loggers.AGENT.info("Starting Boncode analysis");
		//logger.info("Starting Boncode analysis...");

		new BuildServerToMonitorLink(analysisProperties, monitorUploadDirectory, this).perform(sourceRoot);

	}

	public boolean isInterrupted() {
		Loggers.AGENT.info("isInterrupted invoked");
		return false;
	}

	public boolean isFinished() {
		Loggers.AGENT.info("isFinished invoked");
		return false;
	}

	public void interrupt() {
		Loggers.AGENT.info("interrupt invoked");
	}

	@NotNull
	public BuildFinishedStatus waitFor() throws RunBuildException {
		Loggers.AGENT.info("waitFor invoked");

		return BuildFinishedStatus.FINISHED_SUCCESS;
	}

	public void log(LogEntry entry) {

		Loggers.AGENT.info(entry.toString());

		//To change body of implemented methods use File | Settings | File Templates.
	}

	public String getStatus() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void addAppender(org.ijsberg.iglu.logging.Logger appender) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void removeAppender(org.ijsberg.iglu.logging.Logger appender) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
