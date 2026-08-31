package br.com.tresvtintas.lab.autostart;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

public final class ProbeStateStore {
    private static final String FILE_NAME = "f703a_probe_state";
    private static final String KEY_SCENARIO = "scenario";
    private static final String KEY_OUTCOME = "outcome";
    private static final String KEY_EXCEPTION = "exception_class";
    private static final String KEY_ATTEMPTED_AT = "attempted_at_elapsed_ms";
    private static final String KEY_STARTED_AT = "started_at_elapsed_ms";
    private static final String KEY_STOPPED_AT = "stopped_at_elapsed_ms";
    private static final String KEY_BOOT_AT = "boot_at_elapsed_ms";
    private static final String VALUE_NONE = "none";
    private static final String OUTCOME_ATTEMPTED = "attempted";
    private static final String OUTCOME_FOREGROUND_STARTED =
            "foreground_started";
    private static final String OUTCOME_SCHEDULED = "scheduled";
    private static final String OUTCOME_SCHEDULE_REJECTED =
            "schedule_rejected";
    private static final String OUTCOME_START_FAILED = "start_failed";
    private static final String OUTCOME_STOPPED = "stopped";

    private ProbeStateStore() {
    }

    public static void reset(Context context) {
        preferences(context)
                .edit()
                .clear()
                .putString(KEY_SCENARIO, VALUE_NONE)
                .putString(KEY_OUTCOME, VALUE_NONE)
                .putString(KEY_EXCEPTION, VALUE_NONE)
                .apply();
    }

    public static void recordAttempt(Context context, String scenario) {
        preferences(context)
                .edit()
                .putString(KEY_SCENARIO, scenario)
                .putString(KEY_OUTCOME, OUTCOME_ATTEMPTED)
                .putString(KEY_EXCEPTION, VALUE_NONE)
                .putLong(KEY_ATTEMPTED_AT, SystemClock.elapsedRealtime())
                .apply();
    }

    public static void recordScheduled(Context context, String scenario) {
        preferences(context)
                .edit()
                .putString(KEY_SCENARIO, scenario)
                .putString(KEY_OUTCOME, OUTCOME_SCHEDULED)
                .putString(KEY_EXCEPTION, VALUE_NONE)
                .apply();
    }

    public static void recordScheduleRejected(Context context) {
        preferences(context)
                .edit()
                .putString(KEY_OUTCOME, OUTCOME_SCHEDULE_REJECTED)
                .apply();
    }

    public static void recordStarted(Context context) {
        preferences(context)
                .edit()
                .putString(KEY_OUTCOME, OUTCOME_FOREGROUND_STARTED)
                .putLong(KEY_STARTED_AT, SystemClock.elapsedRealtime())
                .apply();
    }

    public static void recordFailure(Context context, RuntimeException failure) {
        preferences(context)
                .edit()
                .putString(KEY_OUTCOME, OUTCOME_START_FAILED)
                .putString(KEY_EXCEPTION, failure.getClass().getName())
                .apply();
    }

    public static void recordStopped(Context context) {
        SharedPreferences values = preferences(context);
        SharedPreferences.Editor editor = values
                .edit()
                .putLong(KEY_STOPPED_AT, SystemClock.elapsedRealtime());
        if (OUTCOME_FOREGROUND_STARTED.equals(
                values.getString(KEY_OUTCOME, VALUE_NONE))) {
            editor.putString(KEY_OUTCOME, OUTCOME_STOPPED);
        }
        editor.apply();
    }

    public static void recordBoot(Context context) {
        preferences(context)
                .edit()
                .putLong(KEY_BOOT_AT, SystemClock.elapsedRealtime())
                .apply();
    }

    public static Snapshot read(Context context) {
        SharedPreferences values = preferences(context);
        return new Snapshot(
                values.getString(KEY_SCENARIO, VALUE_NONE),
                values.getString(KEY_OUTCOME, VALUE_NONE),
                values.getString(KEY_EXCEPTION, VALUE_NONE),
                values.getLong(KEY_ATTEMPTED_AT, 0),
                values.getLong(KEY_STARTED_AT, 0),
                values.getLong(KEY_STOPPED_AT, 0),
                values.getLong(KEY_BOOT_AT, 0));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public static final class Snapshot {
        private final String scenario;
        private final String outcome;
        private final String exceptionClass;
        private final long attemptedAtElapsedMillis;
        private final long startedAtElapsedMillis;
        private final long stoppedAtElapsedMillis;
        private final long bootAtElapsedMillis;

        Snapshot(
                String scenario,
                String outcome,
                String exceptionClass,
                long attemptedAtElapsedMillis,
                long startedAtElapsedMillis,
                long stoppedAtElapsedMillis,
                long bootAtElapsedMillis) {
            this.scenario = scenario;
            this.outcome = outcome;
            this.exceptionClass = exceptionClass;
            this.attemptedAtElapsedMillis = attemptedAtElapsedMillis;
            this.startedAtElapsedMillis = startedAtElapsedMillis;
            this.stoppedAtElapsedMillis = stoppedAtElapsedMillis;
            this.bootAtElapsedMillis = bootAtElapsedMillis;
        }

        public String scenario() {
            return scenario;
        }

        public String outcome() {
            return outcome;
        }

        public String exceptionClass() {
            return exceptionClass;
        }

        public long attemptedAtElapsedMillis() {
            return attemptedAtElapsedMillis;
        }

        public long startedAtElapsedMillis() {
            return startedAtElapsedMillis;
        }

        public long stoppedAtElapsedMillis() {
            return stoppedAtElapsedMillis;
        }

        public long bootAtElapsedMillis() {
            return bootAtElapsedMillis;
        }
    }
}
