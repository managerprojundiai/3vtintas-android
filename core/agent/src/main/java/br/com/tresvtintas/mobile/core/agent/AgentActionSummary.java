package br.com.tresvtintas.mobile.core.agent;

public sealed interface AgentActionSummary
        permits AgentAppointmentSummary,
        AgentAttendanceReplySummary,
        AgentCommissionSummary,
        AgentDeliverySummary,
        AgentFinanceSummary,
        AgentOrderSummary,
        AgentMaterialQuoteAmendSummary,
        AgentMaterialQuoteCreateSummary,
        AgentMaterialQuoteSendSummary { }
