package DTO;

public class Utility {
	public static String JOIN_CHATROOM = "JOIN_CHATROOM";
	public static String SERVER_IP = "SERVER_IP";
	public static String PORT = "PORT";
	public static String ROOM_REF = "ROOM_REF";
	public static String JOIN_ID = "JOIN_ID";
	
	public static String JOINED_CHATROOM = "JOINED_CHATROOM";

	public static String LEAVE_CHATROOM = "LEAVE_CHATROOM";
	public static String LEFT_CHATROOM = "LEFT_CHATROOM";
	
	public static String DISCONNECT = "DISCONNECT";
	public static String CLIENT_NAME = "CLIENT_NAME";

	public static String CHAT = "CHAT";
	public static String MESSAGE = "MESSAGE";

	public static String ERROR_CODE = "ERROR_CODE";
	public static String ERROR_DESCRIPTION = "ERROR_DESCRIPTION";
	
	public static String SEGEMENT = "\n";

	
	public static String dispatchMessage(String message){
		String[] mStrings = message.split("@");
		String m = "";
		for(String string: mStrings){
			m += string + "\n";
		}
		
		return m;
	}
}
