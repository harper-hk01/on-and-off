package common;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * 네트워크로 오가는 메시지 한 줄 {"type":"명령어","data":{...}} 을 자바 객체로 표현
 * 
 * JSON 변환은 이 클래스에서만
 */

public class Message {

	private static final Gson GSON = new GsonBuilder().serializeNulls().create(); // null 값도 "이름": null 로 보내도록

	private final Command type; // 무슨 일인지 (LOGIN, CHAT_RECV...)
	private final Object data; // 필요한 정보 (요청/결과/알림 DTO). 없으면 null

	public Message(Command type, Object data) {
		this.type = type;
		this.data = data;
	}

	public Message(Command type) { // data 없는 메시지용 생성자 (예: LOGOUT)
		this(type, null);
	}

	// ===== 보낼 때: Message → JSON 한 줄 =====
	public String toLine() {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", type.name());
		obj.add("data", data == null ? new JsonObject() : GSON.toJsonTree(data));
		return GSON.toJson(obj);
	}

	// ===== 받을 때: JSON 한 줄 → Message =====
	public static Message parse(String line) {
		try {
			JsonObject obj = JsonParser.parseString(line).getAsJsonObject();

			if (!obj.has("type")) {
				throw new IllegalArgumentException("type이 없습니다.");
			}

			Command type = Command.valueOf(obj.get("type").getAsString());

			Object data = null; // 어떤 명령어의 DTO든 다 받을 수 있는 Object 타입으로 선언

			Type dataType = type.getDataType();
			JsonElement dataEl = obj.get("data");

			if (dataType != null && dataEl != null && !dataEl.isJsonNull()) {
				data = GSON.fromJson(dataEl, dataType);
			}

			return new Message(type, data);
		} catch (JsonParseException e) { // JSON 모양이 아님
			throw new IllegalArgumentException("JSON 형식이 아닙니다.", e);
		} catch (IllegalStateException e) { // JSON이긴 한데 { } 가 아님
			throw new IllegalArgumentException("메시지 형식이 아닙니다.", e);
		}
	}

	// ===== Message 밖에서 Message를 쓰기 위한창구 =====
	public Command getType() { // 무슨 명령어인지 (Dispatcher·Controller의 switch에서 씀)
		return type;
	}

	public Object getData() { // DTO (꺼낼 때 (LoginRequest) 처럼 형변환해서 씀)
		return data;
	}

	@Override
	public String toString() { // 로그 출력용. 비밀번호가 찍히지 않도록 type만
		return "Message[" + type + "]";
	}
}
