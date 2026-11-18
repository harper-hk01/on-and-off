package common;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/* 네트워크로 오가는 메시지 한 줄.
형식: {"type":"명령어","data":{...}}
JSON 변환은 이 클래스에서만!
*/

public class Message { //메시지 한 줄을 표현하는 클래스
	
	// null 값도 "이름": null 로 보내도록 serializeNulls 옵션을 켠 Gson (모든 Message가 하나를 같이 씀)
	private static final Gson GSON = new GsonBuilder().serializeNulls().create();
	
	private final Command type; // 무슨 일인지 (LOGIN, CHAT_RECV...)
	private final Object data; // 필요한 정보 (요청/결과/알림 DTO). 없으면 null
	
    public Message(Command type, Object data) { // type과 data를 모두 받는 생성자
        this.type = type; // 명령어 저장
        this.data = data; // DTO 저장
    }
    
    public Message(Command type) { // data 없는 메시지용 생성자 (예: LOGOUT)
        this(type, null);
    }
    
    // ===== 보낼 때: Message → JSON 한 줄 =====
    public String toLine() { // 이 메시지를 네트워크로 보낼 한 줄 문자열로 만든다
        JsonObject obj = new JsonObject(); // 바깥 껍데기 { } 를 만든다
        obj.addProperty("type", type.name()); // "type": "LOGIN" 처럼 명령어 이름 넣기
        if (data == null) { // 보낼 data가 없으면
            obj.add("data", new JsonObject()); // "data": {} (빈 객체)를 넣기
        } else { // 보낼 data가 있으면
            obj.add("data", GSON.toJsonTree(data)); // DTO를 JSON으로 바꿔 "data": {...} 로 넣기
        }
        return GSON.toJson(obj); // 완성된 { "type":…, "data":… } 를 한 줄 문자열로 돌려줌
    }
    
    // ===== 받을 때: JSON 한 줄 → Message =====
    public static Message parse(String line) { // 이 메시지를 네트워크로 보낼 한 줄 문자열로 만들기
    	try {
        	JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            
            if (!obj.has("type")) {
                throw new IllegalArgumentException("type이 없습니다: " + line);
            }
            
            Command type = Command.valueOf(obj.get("type").getAsString()); // 어떤 명령어인지 알아내기
            
            Object data = null;
            
            Type dataType = type.getDataType(); // 그 명령의 data클래스 알아내기
            JsonElement dataEl = obj.get("data"); // data부분만 꺼내기
            
            // data를 담을 DTO가 정해져 있고, 실제로 data가 왔을 때만 변환
            if (dataType != null && dataEl != null && !dataEl.isJsonNull()) {
                data = GSON.fromJson(dataEl, dataType);// {  } -> LoginRequest 같은 DTO 객체
            }
            
            return new Message(type, data); // 완성된 Message 돌려주기
        } catch (JsonParseException e) { // JSON 모양이 아님
            throw new IllegalArgumentException("JSON 형식이 아닙니다: " + line, e);
        } catch (IllegalStateException e) { // JSON이긴 한데 { } 가 아님
            throw new IllegalArgumentException("메시지 형식이 아닙니다: " + line, e);
        }
    }
    
    // ===== 꺼내기 =====
    public Command getType() { // 무슨 명령어인지 (Dispatcher·Controller의 switch에서 씀)
        return type;
    }

    public Object getData() { // DTO (꺼낼 때 (LoginRequest) 처럼 형변환해서 씀)
        return data;
    }

    @Override
    public String toString() { // 로그 출력용. 비밀번호가 찍히지 않도록 type만 (data는 뺌)
        return "Message[" + type + "]";
    }
}
