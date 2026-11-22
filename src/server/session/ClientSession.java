package server.session;

import common.Command;

public interface ClientSession {
	void send(Command type, Object data);
}
