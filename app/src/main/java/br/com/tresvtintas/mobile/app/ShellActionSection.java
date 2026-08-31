package br.com.tresvtintas.mobile.app;

enum ShellActionSection {
    OVERVIEW(R.string.shell_section_overview),
    SALES(R.string.shell_section_sales),
    OPERATIONS(R.string.shell_section_operations),
    MANAGEMENT(R.string.shell_section_management),
    PERSONAL(R.string.shell_section_personal);

    private final int titleResource;

    ShellActionSection(int titleResource) {
        this.titleResource = titleResource;
    }

    int titleResource() {
        return titleResource;
    }
}
