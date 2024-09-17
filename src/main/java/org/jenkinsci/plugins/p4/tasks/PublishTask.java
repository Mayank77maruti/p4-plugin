package org.jenkinsci.plugins.p4.tasks;

import hudson.AbortException;
import hudson.FilePath.FileCallable;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.security.Roles;
import org.jenkinsci.plugins.p4.client.ClientHelper;
import org.jenkinsci.plugins.p4.publish.Publish;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

public class PublishTask extends AbstractTask implements FileCallable<Boolean>, Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(PublishTask.class.getName());

	private final Publish publish;

	private String publishedChangeID = "Default";

	public PublishTask(String credential, Run<?, ?> run, TaskListener listener, Publish publish) {
		super(credential, run, listener);
		this.publish = publish;
	}

	public Boolean invoke(File workspace, VirtualChannel channel) throws IOException {
		return (Boolean) tryTask();
	}

	@Override
	public Object task(ClientHelper p4) throws Exception {
		try {
			// Check connection (might be on remote slave)
			if (!checkConnection(p4)) {
				return false;
			}

			// Look for changes and add to change-list, then publish
			boolean open = p4.buildChange(publish);
			if (open) {
				this.publishedChangeID = p4.publishChange(publish);
			}
		} catch (Exception e) {
			p4.log("(p4):stop:exception\n");
			String err = "Unable to publish workspace: " + e;
			p4.log(err);
			logger.warning(err);
			throw new AbortException(err);
		}
		return true;
	}

	public void checkRoles(RoleChecker checker) throws SecurityException {
		checker.check(this, Roles.SLAVE);
	}

	public String getPublishedChangeID() {
		return publishedChangeID;
	}
}
