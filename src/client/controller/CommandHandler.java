package client.controller;

import java.awt.Toolkit;

import client.view.ChatView;
import common.Message;


/**
 * 버즈(BUZZ_RECV), 발언 독려(NUDGE_NOTI)처럼 "창에 효과를 주는" 알림 처리 담당.
 * 실제 흔들기/독려 문구 표시는 ChatView(showBuzz, showNudge)가 구현해 두었으므로
 * 여기서는 어떤 효과를 어떤 순서로 줄지만 정한다.
 */

public class CommandHandler {
	public CommandHandler() {}
	
	//ChatController가 부름
	public void handleCommand(Message msg, ChatView view) {
		if(view == null) {
			return;
		}
		
		switch(msg.getType()) {
		case BUZZ_RECV: //일상모드에서 버즈
            bringToFront(view);
            playSound();
            shakeWindow(view);
            break;
            
        case NUDGE_NOTI: { //업무모드에서 독려
            bringToFront(view);
            playSound();
            showNudge(view);
            break;
        }

        default:
            break;
		}
	}
	private void shakeWindow(ChatView view) {
        view.showBuzz();
    }

    private void bringToFront(ChatView view) {
        view.setVisible(true);
        view.toFront();
        view.setAlwaysOnTop(true);
        view.setAlwaysOnTop(false);
        
    }

    private void playSound() {
        Toolkit.getDefaultToolkit().beep();
    }

    // ChatView.showNudge = 문구 표시 + 창 흔들기
    private void showNudge(ChatView view) {
        view.showNudge();
    }
}
