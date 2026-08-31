package br.com.tresvtintas.mobile.core.agent;

public sealed interface AgentActionResult
        permits AgentAppointmentResult,
        AgentAttendanceReplyResult,
        AgentCommissionResult,
        AgentDeliveryResult,
        AgentFinanceResult,
        AgentOrderResult,
        AgentMaterialQuoteAmendResult,
        AgentMaterialQuoteCreateResult,
        AgentMaterialQuoteSendResult { }
