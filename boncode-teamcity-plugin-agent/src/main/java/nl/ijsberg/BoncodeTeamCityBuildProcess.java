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
public class BoncodeTeamCityBuildProcess implements BuildProcess {

	private BuildRunnerContext context;

	private static final Logger logger = Logger.getLogger(BoncodeTeamCityBuildProcess.class);

	private String monitorUploadDirectory;
    private String monitorDownloadDirectory;

    private String analysisProperties;
	private String sourceRoot;
	private String checkoutDir;



	public BoncodeTeamCityBuildProcess(BuildRunnerContext context, Logger logger) {		
		this.context = context;
		this.logger = logger;
		Map<String, String> configParameters = context.getConfigParameters();
		Map<String, String> buildParameters = context.getConfigParameters();
		logger.log(new LogEntry("config parameters: " + configParameters));
		logger.log(new LogEntry("build parameters: " + buildParameters));
		logger.log(new LogEntry("Checkout dir: " + context.getBuild().getCheckoutDirectory()));

		checkoutDir = context.getBuild().getCheckoutDirectory().getAbsolutePath();
		sourceRoot = checkoutDir + "/" + buildParameters.get(BuildServerToMonitorLink.SOURCE_ROOT);
		monitorUploadDirectory = buildParameters.get(BuildServerToMonitorLink.MONITOR_UPLOAD_DIRECTORY);
        monitorDownloadDirectory = buildParameters.get(BuildServerToMonitorLink.MONITOR_DOWNLOAD_DIRECTORY);

        analysisProperties = buildParameters.get(BuildServerToMonitorLink.ANALYSIS_PROPERTIES_FILENAME);

		BuildServerToMonitorLink.throwIfPropertiesNotOk(
				analysisProperties,
				monitorUploadDirectory,
				sourceRoot);
	}

	public void start() throws RunBuildException {
		logger.log(new LogEntry("Starting Boncode analysis"));
		//logger.info("Starting Boncode analysis...");

        boolean result = new BuildServerToMonitorLink(analysisProperties, monitorUploadDirectory, monitorDownloadDirectory, this).perform(sourceRoot);
        if(result){
            this.isFinished();
        } else {
            this.isInterrupted();
        }

	}

	public boolean isInterrupted() {
		logger.log(new LogEntry("isInterrupted invoked"));
		return false;
	}

	public boolean isFinished() {
		logger.log(new LogEntry("isFinished invoked"));
		return false;
	}

	public void interrupt() {
		logger.log(new LogEntry("interrupt invoked"));
	}

	@NotNull
	public BuildFinishedStatus waitFor() throws RunBuildException {
		logger.log(new LogEntry("waitFor invoked"));

		return BuildFinishedStatus.FINISHED_SUCCESS;
	}

}
