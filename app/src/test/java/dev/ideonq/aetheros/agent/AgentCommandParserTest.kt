package dev.ideonq.aetheros.agent

import org.junit.Assert.*
import org.junit.Test

class AgentCommandParserTest {
    @Test fun parsesKakao() {
        val result = AgentCommandParser.parse("엄마한테 카톡으로 오늘 늦는다고 보내줘")
        assertEquals(AgentCommand.Message(Channel.KAKAO, "엄마", "오늘 늦는다고"), result)
    }
    @Test fun parsesSms() {
        assertEquals(
            AgentCommand.Message(Channel.SMS, "아빠", "도착했어"),
            AgentCommandParser.parse("문자로 아빠에게 도착했어 보내줘")
        )
    }
    @Test fun parsesOpen() {
        assertEquals(AgentCommand.OpenApp("카메라"), AgentCommandParser.parse("카메라 열어줘"))
    }
    @Test fun avoidsArbitraryCommands() {
        assertEquals(AgentCommand.Unrecognized, AgentCommandParser.parse("시스템 서비스 전부 종료해줘"))
        assertEquals(AgentCommand.Unrecognized, AgentCommandParser.parse(""))
    }
}
