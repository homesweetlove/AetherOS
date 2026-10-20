package dev.ideonq.aetheros.agent

import org.junit.Assert.*
import org.junit.Test

class GatewayPlanTest {
    @Test fun parsesSupportedPlan() {
        val json = """{"reply":"준비했어요","actions":[{"type":"message","channel":"kakao","recipient":"엄마","body":"오늘 늦어","app":"","setting":""}]}"""
        val p = GptGatewayClient.decodePlan(json)
        assertEquals("준비했어요", p.reply)
        assertEquals(AgentCommand.Message(Channel.KAKAO, "엄마", "오늘 늦어"), p.actions.single())
    }

    @Test fun rejectsUnsupportedToolPlan() {
        val json = """{"reply":"x","actions":[{"type":"shell","app":"rm","channel":"","recipient":"","body":"","setting":""}]}"""
        assertThrows(IllegalStateException::class.java) { GptGatewayClient.decodePlan(json) }
    }

    @Test fun limitsActions() {
        val json = """{"reply":"ok","actions":[{ "type":"open_app","app":"카메라" },{ "type":"open_app","app":"카메라" },{ "type":"open_app","app":"카메라" },{ "type":"open_app","app":"카메라" },{ "type":"open_app","app":"카메라" }]}"""
        assertThrows(IllegalStateException::class.java) { GptGatewayClient.decodePlan(json) }
    }
}
