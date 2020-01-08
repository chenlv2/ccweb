package ccait.ccweb.listener;

import me.javaroad.openapi.wechat.mp.event.ReceiveMessageEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ReceiveMessageEventListener implements ApplicationListener<ReceiveMessageEvent> {

    @Async
    @Override
    public void onApplicationEvent(ReceiveMessageEvent event) {
        System.out.println(event.getSource());
    }
}
