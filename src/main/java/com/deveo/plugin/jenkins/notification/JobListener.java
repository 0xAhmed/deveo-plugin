package com.deveo.plugin.jenkins.notification;

import java.util.Properties;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

@Extension
@SuppressWarnings("rawtypes")
public class JobListener extends RunListener<Run> {

	public JobListener() {
		super(Run.class);
	}

//	@Override
//	public void onStarted(Run r, TaskListener listener) {
//		Phase.STARTED.handlePhase(r, getStatus(r), listener);
//	}
//
//	@Override
//	public void onCompleted(Run r, TaskListener listener) {
//		Phase.COMPLETED.handlePhase(r, getStatus(r), listener);
//	}
//
	@Override
	public void onFinalized(Run r) {
		Properties p = System.getProperties();
		StringBuffer  sb = new StringBuffer();
		for (Object key: p.keySet()) {
			
			sb.append(key+" :: ");
		}
		String result = sb.toString();
		Phase.FINISHED.handlePhase(r, getStatus(r), TaskListener.NULL);
	}

	private String getStatus(Run r) {
		Result result = r.getResult();
		String status = null;
		if (result != null) {
			status = result.toString();
		}
		return status;
	}
}