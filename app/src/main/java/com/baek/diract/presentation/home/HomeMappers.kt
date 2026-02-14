package com.baek.diract.presentation.home

import com.baek.diract.domain.model.TeamspaceSummary
import com.baek.diract.presentation.common.option.TeamspaceUi

/**
 * TODO: TeamspaceUi 생성자/필드에 맞게 여기만 수정하면 됨.
 */
fun TeamspaceSummary.toUi(): TeamspaceUi {
    // ✅ 예시 1) TeamspaceUi(id, name) 형태라면:
    // return TeamspaceUi(id = this.id, name = this.name)

    // ✅ 예시 2) TeamspaceUi(teamspaceId, teamspaceName) 형태라면:
    // return TeamspaceUi(teamspaceId = this.id, teamspaceName = this.name)

    // ✅ 예시 3) owner 여부까지 필요하면 (TeamspaceSummary에 owner 관련 필드가 있을 때만)
    // return TeamspaceUi(id = this.id, name = this.name, isOwner = this.isOwner)

    // -----
    // 지금은 컴파일 강제용 임시 (여기서 반드시 너 프로젝트에 맞게 수정)
    throw IllegalStateException("TeamspaceSummary.toUi()를 TeamspaceUi 정의에 맞게 구현하세요.")
}
