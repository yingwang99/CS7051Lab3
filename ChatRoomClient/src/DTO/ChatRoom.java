package DTO;

public class ChatRoom {
	private int chatRoomId;
	private String chatRoomName;
	
	public ChatRoom(int chatRoomId, String chatRoomName) {
		super();
		this.chatRoomId = chatRoomId;
		this.chatRoomName = chatRoomName;
	}
	public int getChatRoomId() {
		return chatRoomId;
	}
	public void setChatRoomId(int chatRoomId) {
		this.chatRoomId = chatRoomId;
	}
	public String getChatRoomName() {
		return chatRoomName;
	}
	public void setChatRoomName(String chatRoomName) {
		this.chatRoomName = chatRoomName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + chatRoomId;
		result = prime * result + ((chatRoomName == null) ? 0 : chatRoomName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChatRoom other = (ChatRoom) obj;
		if (chatRoomId != other.chatRoomId)
			return false;
		if (chatRoomName == null) {
			if (other.chatRoomName != null)
				return false;
		} else if (!chatRoomName.equals(other.chatRoomName))
			return false;
		return true;
	}
	
	
}
