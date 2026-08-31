package br.com.tresvtintas.lab.autostart;

public final class ProbeContract {
    public static final String EXTRA_COMMAND = "br.com.tresvtintas.lab.autostart.COMMAND";
    public static final String COMMAND_IMMEDIATE = "immediate";
    public static final String COMMAND_SCHEDULE = "schedule";
    public static final String COMMAND_RESET = "reset";
    public static final String SCENARIO_VISIBLE = "visible_activity";
    public static final String SCENARIO_BACKGROUND_JOB = "background_job";
    public static final String SCENARIO_BOOT = "boot_completed";

    private ProbeContract() {
    }
}
