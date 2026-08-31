package br.com.tresvtintas.lab.autostart;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class ProbeJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters parameters) {
        ProbeStarter.start(this, ProbeContract.SCENARIO_BACKGROUND_JOB);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return false;
    }
}
