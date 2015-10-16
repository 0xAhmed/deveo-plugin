package com.deveo.plugin.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;

public class DeveoNotifier extends Notifier {

    private final String accountKey;

    private final DeveoRepository repository;

    @DataBoundConstructor
    public DeveoNotifier(String accountKey, String projectId, String repositoryId) {
        this.accountKey = accountKey;
        this.repository = new DeveoRepository(projectId, repositoryId);
    }

    public String getAccountKey() {
        return accountKey;
    }

    public String getProjectId() {
        return repository.getProjectId();
    }

    public String getRepositoryId() {
        return repository.getId();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars environment = getEnvironment(build, listener);
        notifyDeveo(build, listener, environment);
        return true;
    }

    private EnvVars getEnvironment(AbstractBuild build, BuildListener listener) {
        EnvVars environment = null;

        try {
            environment = build.getEnvironment(listener);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return environment;
    }

    private boolean isGit(EnvVars environment) {
        return StringUtils.isNotBlank(environment.get("GIT_COMMIT"));
    }

    private String getRevisionId(EnvVars environment) {
        return isGit(environment) ? environment.get("GIT_COMMIT") : environment.get("SVN_REVISION");
    }

    private String getBuildUrl(EnvVars environment) {
        return environment.get("BUILD_URL");
    }

    private String getOperation(AbstractBuild build) {
        return build.getResult() == Result.SUCCESS ? "completed" : "failed";
    }

    private String getJobName(AbstractBuild build) {
        return build.getProject().getDisplayName();
    }

    private String getRef(EnvVars environment) {
        return isGit(environment) ? environment.get("GIT_BRANCH").replace("origin/", "") : "";
    }

    private DeveoAPIKeys getApiKeys(DeveoBuildStepDescriptor descriptor) {
        return new DeveoAPIKeys(descriptor.getPluginKey(), descriptor.getCompanyKey(), accountKey);
    }

    public void notifyDeveo(AbstractBuild build, BuildListener listener, EnvVars environment) {
        String operation = getOperation(build);
        String jobName = getJobName(build);
        String ref = getRef(environment);
        String revisionId = getRevisionId(environment);
        String buildUrl = getBuildUrl(environment);

        DeveoAPI api = new DeveoAPI(getDescriptor().getHostname(), getApiKeys(getDescriptor()));
        DeveoEvent event = new DeveoEvent(operation, jobName, repository, ref, revisionId, buildUrl);

        try {
            api.create("events", event.toJSON());
        } catch (DeveoException ex) {
            PrintStream logger = listener.getLogger();
            logger.println("Deveo: Failed to create event.");
            logger.println(String.format("Deveo: %s", ex.getMessage()));
        }
    }

    @Override
    public DeveoBuildStepDescriptor getDescriptor() {
        return (DeveoBuildStepDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class DeveoBuildStepDescriptor extends BuildStepDescriptor<Publisher> {

        private String hostname = "https://deveo.com";
        private String pluginKey = "3c94d47d6257ca0d3bc54a9b6a91aa64";
        private String companyKey = "";

        public DeveoBuildStepDescriptor() {
            load();
        }

        public String getHostname() {
            return hostname;
        }

        public String getPluginKey() {
            return pluginKey;
        }

        public String getCompanyKey() {
            return companyKey;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Deveo Notification";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            this.hostname = json.getString("hostname");
            this.pluginKey = json.getString("pluginKey");
            this.companyKey = json.getString("companyKey");

            save();

            return super.configure(req, json);
        }

    }

}
