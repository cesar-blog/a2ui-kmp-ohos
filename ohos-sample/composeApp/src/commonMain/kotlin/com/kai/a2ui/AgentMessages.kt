package com.kai.a2ui

/** Fixed wire messages, processed by the real A2UI parser and renderer. No network or LLM. */
object AgentMessages {
    val booking = listOf(
        """{"version":"v0.9","createSurface":{"surfaceId":"booking","catalogId":"$KAI_CATALOG_ID"}}""",
        """{"version":"v0.9","updateDataModel":{"surfaceId":"booking","path":"/guest","value":"Ada"}}""",
        """{"version":"v0.9","updateDataModel":{"surfaceId":"booking","path":"/party","value":"2"}}""",
        """{"version":"v0.9","updateComponents":{"surfaceId":"booking","components":[
          {"id":"root","component":"Panel","children":["title","place","guest","party","submit"]},
          {"id":"title","component":"Heading","text":"预订餐桌"},
          {"id":"place","component":"Label","text":"KAI 聚餐 · 今晚 19:30"},
          {"id":"guest","component":"Input","label":"姓名","value":{"path":"/guest"}},
          {"id":"party","component":"Party","value":{"path":"/party"}},
          {"id":"submit","component":"Action","label":"确认预订",
            "action":{"event":{"name":"book_table","context":{
              "guest":{"path":"/guest"},"party":{"path":"/party"}
            }}}}
        ]}}""",
    )

    val updates = listOf(
        0L to """{"version":"v0.9","createSurface":{"surfaceId":"order","catalogId":"$KAI_CATALOG_ID"}}""",
        0L to """{"version":"v0.9","updateDataModel":{"surfaceId":"order","path":"/status","value":"已收到订单"}}""",
        0L to """{"version":"v0.9","updateComponents":{"surfaceId":"order","components":[
          {"id":"root","component":"Panel","children":["title","status"]},
          {"id":"title","component":"Heading","text":"订单状态"},
          {"id":"status","component":"Label","text":{"path":"/status"}}
        ]}}""",
        1300L to """{"version":"v0.9","updateDataModel":{"surfaceId":"order","path":"/status","value":"正在制作"}}""",
        1300L to """{"version":"v0.9","updateDataModel":{"surfaceId":"order","path":"/status","value":"可以取餐了"}}""",
        0L to """{"version":"v0.9","updateComponents":{"surfaceId":"order","components":[
          {"id":"root","component":"Panel","children":["title","status","pickup"]},
          {"id":"pickup","component":"Action","label":"确认取餐",
            "action":{"event":{"name":"confirm_pickup","context":{"status":{"path":"/status"}}}}}
        ]}}""",
    )
}
