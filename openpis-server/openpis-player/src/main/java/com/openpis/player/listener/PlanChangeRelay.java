package com.openpis.player.listener;

import com.openpis.common.event.PlanChangedEvent;
import com.openpis.player.ws.PlayerWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanChangeRelay {

    private final PlayerWebSocketHandler playerWebSocketHandler;

    @EventListener
    public void onPlanChanged(PlanChangedEvent event) {
        playerWebSocketHandler.broadcast("{\"type\":\"PLAN_CHANGED\"}");
    }
}
