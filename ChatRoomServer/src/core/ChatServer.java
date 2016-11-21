package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.ClientInfoStatus;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.rmi.CORBA.Util;
import javax.sound.midi.MidiDevice.Info;

import DTO.ChatRoom;
import DTO.User;
import DTO.Utility;

public class ChatServer {
	private static ExecutorService executorService = null;
	public static ArrayList<ChatRoom> chatRooms = new ArrayList<ChatRoom>();
	public static HashMap<String, ServerThread> userMap = new HashMap<String, ServerThread>();

	final int POOL_SIZE=10;

	private ServerSocket serverSocket = null;
	private String localIp = "";

	static int id = 0;
	static int mapId = 0;

	public ChatServer() {

		try {
	        executorService=Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*POOL_SIZE);
			serverSocket = new ServerSocket(54321);
			localIp = InetAddress.getLocalHost().getHostAddress();
			System.out.println("Server start");
			while (true) {

				Socket cs = serverSocket.accept();
				ServerThread thread = new ServerThread(cs);
				executorService.execute(thread);
				id++;
				

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new ChatServer();
	}

	public void respondJoin(String[] mString, ServerThread s, PrintWriter writer) throws IOException {

		boolean check = false;
		String respond = "";
		String joinRoom = mString[0].split(":")[1];

		System.out.println(checkJoin(joinRoom.trim(), s));
		if (checkJoin(joinRoom.trim(), s) == false) {
			int roomRef = 0;

			ChatRoom chatRoom = null;

			if (checkChatRoom(joinRoom) == false) {
				chatRoom = new ChatRoom(chatRooms.size(), joinRoom);
				synchronized (chatRooms) {
					chatRooms.add(chatRoom);
				}
			}

			roomRef = chatRoom.getChatRoomId();

			respond = Utility.JOINED_CHATROOM + ":" + joinRoom + Utility.SEGEMENT + Utility.SERVER_IP + ":" + localIp
					+ Utility.SEGEMENT + Utility.PORT + ":" + 54321 + Utility.SEGEMENT + Utility.ROOM_REF + ":"
					+ roomRef + Utility.SEGEMENT + Utility.JOIN_ID + ":" + id;

			mapId++;
			synchronized (userMap) {
				userMap.put(joinRoom + ":" + mapId, s);
				System.out.println("join room: " + joinRoom);
				System.out.println("user map size: " + userMap.size());
			}

			writer.println(respond);

			String joinInform = Utility.CHAT + ":" + roomRef + Utility.SEGEMENT + mString[3] + Utility.SEGEMENT
					+ Utility.MESSAGE + ":" + mString[3].split(":")[1]
					+ " has joined this chatroom.\n";
			pushToAll(joinRoom, joinInform, s, writer);

		} else {
			respond = Utility.ERROR_CODE + ":0" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION + ":"
					+ "You have joined the chatroom!";
			writer.println(respond);
		}

		writer.flush();

	}

	public synchronized boolean checkChatRoom(String room) {
		for (ChatRoom chatRoom : chatRooms) {
			if (chatRoom.equals(room)) {
				return true;
			}
		}
		return false;
	}

	public synchronized boolean checkJoin(String room, ServerThread server) {
		Iterator iter = userMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			String key = (String) entry.getKey();
			ServerThread serverThread = (ServerThread) entry.getValue();
			if (room.equals(key.split(":")[0]) && serverThread.equals(server)) {
				return true;
			}
		}

		return false;

	}

	public synchronized void pushToAll(String room, String msg, ServerThread s, PrintWriter writer) throws IOException {
		Iterator iter = userMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			String key = (String) entry.getKey();
			ServerThread serverThread = (ServerThread) entry.getValue();
			System.out.println(key.split(":")[0]);
			System.out.println("check chat room: " + room);
			
			if (key.split(":")[0].equals(room)) {	
				writer = getWriter(serverThread.getSocket());
				writer.println(msg);
			}
		}
		writer.flush();

	}

	public String respondLeave(String[] mString) {

		return Utility.LEFT_CHATROOM + ":" + mString[0].split(":")[1] + Utility.SEGEMENT
				+ Utility.JOIN_ID + ":" + mString[1].split(":")[1];

	}

	public PrintWriter getWriter(Socket socket) throws IOException {
		OutputStream socketOut = socket.getOutputStream();
		return new PrintWriter(socketOut, true);
	}

	public BufferedReader getReader(Socket socket) throws IOException {
		InputStream socketIn = socket.getInputStream();
		return new BufferedReader(new InputStreamReader(socketIn));
	}

	class ServerThread extends Thread {
		private Socket socket;
		private BufferedReader reader;
		private PrintWriter writer;

		public ServerThread(Socket socket) throws IOException {
			this.socket = socket;
			reader = getReader(socket);
			writer = getWriter(socket);
		}

		@SuppressWarnings("deprecation")
		public void run() {

			String info;
			try {
				while ((info = reader.readLine()) != null) {
					
					if (info.startsWith(Utility.JOIN_CHATROOM)) {
						String[] mString = addToString(4, info);
						respondJoin(mString, this, writer);
					} else if (info.startsWith(Utility.LEAVE_CHATROOM)) {
						String[] mString = addToString(3, info);
						try {
							System.out.println(info.substring(info.indexOf(" ") + 1));
							int roomR = Integer.parseInt(info.trim().substring(info.indexOf(" ") + 1));
							String leave = chatRooms.get(roomR).getChatRoomName();
							boolean check = false;
							Iterator iter = userMap.entrySet().iterator();
							while (iter.hasNext()) {
								Map.Entry entry = (Map.Entry) iter.next();
								String key = (String) entry.getKey();
								ServerThread value = (ServerThread) entry.getValue();
								if (key.substring(0, key.indexOf("%%")).equals(leave) && value.equals(this)) {
									System.out.println(respondLeave(mString));
									writer.println(respondLeave(mString));
									writer.flush();
									check = true;
									String leaveMsg = Utility.CHAT + ":" + roomR + Utility.SEGEMENT + mString[2]
											+ Utility.SEGEMENT + Utility.MESSAGE + ":"
											+ mString[2].split(":")[1] 
											+ " has left this chatroom.\n";
									pushToAll(leave, leaveMsg, this, writer);
									synchronized (userMap) {
										System.out.println(userMap.size());
										userMap.remove(key);
										System.out.println(userMap.size());
									}

									break;
								}
							}

							if (check == false) {
								writer.println(Utility.ERROR_CODE + ":3" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
										+ ":" + "You didn't join that chatroom\n");

								writer.flush();
							}
						} catch (NumberFormatException e) {
							// TODO: handle exception
							writer.println(Utility.ERROR_CODE + ":3" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
									+ ":" + "Invalid room reference!");
							writer.flush();
						}

					} else if (info.startsWith(Utility.CHAT)) {
						String[] mString = addToString(4, info);
						try {
							int index = Integer.parseInt(info.split(":")[1].trim());
							String chatRoomName = chatRooms.get(index).getChatRoomName();

							pushToAll(chatRoomName,
									info + Utility.SEGEMENT + mString[2] + Utility.SEGEMENT + mString[3] + "\n\n", this, writer);

						} catch (NullPointerException e) {
							writer.println(Utility.ERROR_CODE + ":2" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
									+ ":" + "Invalid room reference!");
							writer.flush();
						} catch (NumberFormatException e) {
							// TODO: handle exception
							writer.println(Utility.ERROR_CODE + ":3" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
									+ ":" + "Invalid room reference!");
							writer.flush();
						}

					} else if (info.startsWith(Utility.DISCONNECT)) {
						addToString(3, info);
						if (!socket.isClosed()) {
							socket.close();
						}
						this.stop();
					} else {
						writer.println(
								info + "\n" + "IP:" + localIp + "\n" + "Port: " + 54321 + "\nStudentID: 16308222");
						writer.flush();
						reader.readLine();
					}
				}

				reader.close();
				writer.close();
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

		private String[] addToString(int size, String start) throws IOException {
			String[] mString = new String[size];
			mString[0] = start;
			System.out.println(start);
			for (int i = 1; i < size; i++) {
				mString[i] = reader.readLine();

				System.out.println(mString[i]);

			}

			if (start.startsWith(Utility.CHAT)) {
				reader.readLine();
				reader.readLine();
			}
			return mString;
		}

		public Socket getSocket() {
			return socket;
		}

		public void setSocket(Socket socket) {
			this.socket = socket;
		}

	}

}
