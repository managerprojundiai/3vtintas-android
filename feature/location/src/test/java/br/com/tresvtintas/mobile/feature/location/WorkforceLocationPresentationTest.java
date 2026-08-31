package br.com.tresvtintas.mobile.feature.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class WorkforceLocationPresentationTest {
    @Test
    public void outsideScheduleAlwaysUsesPausedExplanation() {
        assertEquals(
                "Outside the commercial schedule must remain explicit",
                R.string.location_status_detail_paused,
                WorkforceLocationPresentation.serviceDetail(false, "stopped"));
    }

    @Test
    public void activeStateUsesHumanReadableExplanation() {
        assertEquals(
                "The active service must use the localized explanation",
                R.string.location_status_detail_active,
                WorkforceLocationPresentation.serviceDetail(true, "active"));
    }

    @Test
    public void transitionalStateDoesNotExposeInternalValue() {
        assertEquals(
                "Unknown internal states must be presented as preparation",
                R.string.location_status_detail_starting,
                WorkforceLocationPresentation.serviceDetail(true, "stopped"));
    }

    @Test
    public void blockedStateGivesActionableGuidance() {
        assertEquals(
                "Blocked tracking must guide the user to permissions",
                R.string.location_status_detail_blocked,
                WorkforceLocationPresentation.serviceDetail(true, "blocked"));
    }
}
