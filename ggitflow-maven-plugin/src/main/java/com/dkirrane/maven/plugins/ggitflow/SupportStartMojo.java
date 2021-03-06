/*
 * Copyright 2014 desmondkirrane.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dkirrane.maven.plugins.ggitflow;

import com.dkirrane.gitflow.groovy.GitflowSupport;
import com.dkirrane.gitflow.groovy.ex.GitCommandException;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Creates a new support branch from a specific commit on the master branch.
 */
@Mojo(name = "support-start", aggregator = true)
public class SupportStartMojo extends AbstractSupportMojo {

    /**
     * The commit to start the support branch from.
     *
     * @since 1.2
     */
    @Parameter(property = "startCommit", defaultValue = "", required = false)
    protected String startCommit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        String prefix = getGitflowInit().getSupportBranchPrefix();
        String masterBranch = getGitflowInit().getMasterBranch();

        List<String> localTags = getGitflowInit().gitLocalTagsOnBranch(masterBranch);
        if (localTags.isEmpty()) {
            exceptionMapper.handle(new MojoFailureException("Could not find any local Git tags (on master) to create support branch from!"));
        }

//        if (StringUtils.isBlank(supportName)) {
//            try {
//                supportName = prompter.promptWithDefault("Enter a support branch name (optional, examples SP1, SP2)? ", "");
//                supportName = namer.trimRefName(supportName);
//            } catch (IOException ex) {
//                exceptionMapper.handle(new MojoExecutionException("Error reading support name from command line " + ex.getMessage(), ex));
//            }
//        }

        if (StringUtils.isBlank(startCommit)) {
            try {
                startCommit = prompter.promptChoice("Tags", "Please select a tag to create Support branch from", localTags);
            } catch (IOException ex) {
                exceptionMapper.handle(new MojoFailureException("Error reading tag name from command line " + ex.getMessage()));
            }
        }

        getGitflowInit().executeLocal("git checkout " + startCommit);
        reloadReactorProjects();
        String supportVersion = getSupportVersion(project.getVersion());
        String supportSnapshotVersion = getSupportSnapshotVersion(project.getVersion());
        String supportBranchName = namer.getBranchName(prefix, supportName, supportVersion);

        getLog().info("Starting support branch '" + supportBranchName + "'");
        getLog().debug("msgPrefix '" + getMsgPrefix() + "'");
        getLog().debug("msgSuffix '" + getMsgSuffix() + "'");

        GitflowSupport gitflowSupport = new GitflowSupport();
        gitflowSupport.setInit(getGitflowInit());
        gitflowSupport.setMsgPrefix(getMsgPrefix());
        gitflowSupport.setMsgSuffix(getMsgSuffix());
        gitflowSupport.setPush(true);
        gitflowSupport.setStartCommit(startCommit);

        try {
            gitflowSupport.start(supportBranchName);
        } catch (GitCommandException gce) {
            String header = "Failed to run support start";
            exceptionMapper.handle(header, gce);
        } catch (GitflowException ge) {
            String header = "Failed to run support start";
            exceptionMapper.handle(header, ge);
        }

        setVersion(supportSnapshotVersion, supportBranchName, false);

        if (getGitflowInit().gitRemoteBranchExists(supportBranchName)) {
            getGitflowInit().executeRemote("git push " + getGitflowInit().getOrigin() + " " + supportBranchName);
        }
    }

}
