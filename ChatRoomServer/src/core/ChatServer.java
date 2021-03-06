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
	static boolean setDown = false;

	public ChatServer() throws IOException {

		try {
	        executorService=Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*POOL_SIZE);
			serverSocket = new ServerSocket(54321);
			localIp = InetAddress.getLocalHost().getHostAddress();
			System.out.println("Server start");
			while (true) {

				Socket cs = serverSocket.accept();
				 if(setDown == true) {
					    
	            		break;
	            	}
				ServerThread thread = new ServerThread(cs,id);
				id++;
				executorService.execute(thread);
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			 if(serverSocket != null){
             	serverSocket.close();
             }
		}
		executorService.shutdown();
	    serverSocket.close();
		

	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		new ChatServer();
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
		private int join_id;
		private String client_name = "";

		public ServerThread(Socket socket,int join_id) throws IOException {
			this.socket = socket;
			reader = getReader(socket);
			writer = getWriter(socket);
			this.join_id = join_id;
		}

		

		@SuppressWarnings("deprecation")
		public void run() {

			String info;
			try {
				while ((info = reader.readLine()) != null) {
					
					if (info.startsWith(Utility.JOIN_CHATROOM)) {
						String[] mString = addToString(4, info);
						
						if(this.getClient_name().equals("")){
							this.setClient_name(mString[3].split(":")[1]);
						}
						
						respondJoin(mString, this, writer);
					} else if (info.startsWith(Utility.LEAVE_CHATROOM)) {
						String[] mString = addToString(3, info);
						try {
							int roomR = Integer.parseInt(info.trim().substring(info.indexOf(" ") + 1));
							String leave = chatRooms.get(roomR).getChatRoomName();
							boolean check = false;
							Iterator iter = userMap.entrySet().iterator();
							while (iter.hasNext()) {
								Map.Entry entry = (Map.Entry) iter.next();
								String key = (String) entry.getKey();
								ServerThread value = (ServerThread) entry.getValue();
								if (key.substring(0, key.indexOf(":")).equals(leave) && value.equals(this)) {
									System.out.println(respondLeave(mString));
									writer.println(respondLeave(mString));
									writer.flush();
									check = true;
									String leaveMsg = leaveMsgFormate(mString, roomR);
									pushToAll(leave, leaveMsg, this, writer);
									synchronized (userMap) {
										userMap.remove(key);
									}

									break;
								}
							}

							if (check == false) {
								writer.println(Utility.ERROR_CODE + ":3" + Utility.SEGEMENT + Utility.ERROR_DESCRIPTION
										+ ":" + "You didn't join that chatroom");

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
							for(ChatRoom chatRoom: chatRooms){
								System.out.println(chatRoom.toString());
							}
							String chatRoomName = chatRooms.get(index).getChatRoomName();
							
							pushToAll(chatRoomName,
									info + Utility.SEGEMENT + mString[2] + Utility.SEGEMENT + mString[3] + "\n", this, writer);
						
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
						
						String[] mStrings = addToString(3, info);
						HashMap<String, ServerThread> removeMap = new HashMap<String, ServerThread>();
						
						
						Iterator iter = userMap.entrySet().iterator();
						while (iter.hasNext()) {
							Map.Entry entry = (Map.Entry) iter.next();
							String key = (String) entry.getKey();
							ServerThread value = (ServerThread) entry.getValue();
							if(mStrings[2].split(":")[1].equals(value.getClient_name())){
								removeMap.put(key,value);
								pushToAll(key.split(":")[0], leaveMsgFormate(mStrings, Integer.parseInt(key.split(":")[2])), this, writer);
							}
						}
						synchronized (userMap) {
							Iterator iter2 = removeMap.entrySet().iterator();
							while (iter2.hasNext()) {
								Map.Entry entry = (Map.Entry) iter2.next();
								String key = (String) entry.getKey();
								ServerThread value = (ServerThread) entry.getValue();
								userMap.remove(key);
								
							}
						}
						
						
					} else if(info.startsWith("HELO BASE_TEST")){
						writer.println(
								info + "\n" + "IP:" + localIp + "\n" + "Port: " + 54321 + "\nStudentID: 16308222");
						writer.flush();
					}else if(info.startsWith("KILL_SERVICE")){
								  
					      setDown = true;
						  new Socket("localhost", 54321);
						 
							break;
			        			  
			  	            	}
				}
				socket.close();
				reader.close();
				writer.close();
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}



		private String leaveMsgFormate(String[] mString, int roomR) {
			String leaveMsg = Utility.CHAT + ":" + roomR + Utility.SEGEMENT + mString[2]
					+ Utility.SEGEMENT + Utility.MESSAGE + ":"
					+ mString[2].split(":")[1] 
					+ " has left this chatroom.\n";
			return leaveMsg;
		}

		private String[] addToString(int size, String start) throws IOException {
			String[] mString = new String[size];
			mString[0] = start;
			System.out.println(start);
			for (int i = 1; i < size; i++) {
				mString[i] = reader.readLine();

				System.out.println(mString[i]);

			}

			return mString;
		}
		
		public void respondJoin(String[] mString, ServerThread s, PrintWriter writer) throws IOException {

			String respond = "";
			String joinRoom = mString[0].split(":")[1];

			System.out.println(checkJoin(joinRoom.trim(), s));
			if (checkJoin(joinRoom.trim(), s) == false) {
				int roomRef = 0;

				ChatRoom chatRoom = null;

				if (checkChatRoom(joinRoom) == null) {
					chatRoom = new ChatRoom(chatRooms.size(), joinRoom);
			
					synchronized (chatRooms) {
						chatRooms.add(chatRoom);
					}

				}else{
					chatRoom = checkChatRoom(joinRoom);
				}

				roomRef = chatRoom.getChatRoomId();

				mapId++;
				respond = Utility.JOINED_CHATROOM + ":" + joinRoom + Utility.SEGEMENT + Utility.SERVER_IP + ":" + localIp
						+ Utility.SEGEMENT + Utility.PORT + ":" + 54321 + Utility.SEGEMENT + Utility.ROOM_REF + ":"
						+ roomRef + Utility.SEGEMENT + Utility.JOIN_ID + ":" + mapId;

				
				synchronized (userMap) {
					
					userMap.put(joinRoom + ":" + mapId + ":"+roomRef, s);
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

		private synchronized ChatRoom checkChatRoom(String room) {
			for (ChatRoom chatRoom : chatRooms) {
				if (chatRoom.getChatRoomName().equals(room)) {
					return chatRoom;
				}
			}
			return null;
		}

		private synchronized boolean checkJoin(String room, ServerThread server) {
			Iterator iter = userMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
				System.out.println("key: " + key);
				ServerThread serverThread = (ServerThread) entry.getValue();
				if (room.equals(key.split(":")[0]) && serverThread.equals(server)) {
					return true;
				}
			}

			return false;

		}

		private synchronized void pushToAll(String room, String msg, ServerThread s, PrintWriter writer) throws IOException {
			Iterator iter = userMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
				ServerThread serverThread = (ServerThread) entry.getValue();
				
				if (key.split(":")[0].equals(room)) {	
					System.out.println(key);
					writer = getWriter(serverThread.getSocket());
					writer.println(msg);
				}
			}
			

		}

		private String respondLeave(String[] mString) {

			return Utility.LEFT_CHATROOM + ":" + mString[0].split(":")[1] + Utility.SEGEMENT
					+ Utility.JOIN_ID + ":" + mString[1].split(":")[1];

		}

		public Socket getSocket() {
			return socket;
		}

		public void setSocket(Socket socket) {
			this.socket = socket;
		}



		public int getJoin_id() {
			return join_id;
		}



		public void setJoin_id(int join_id) {
			this.join_id = join_id;
		}



		public String getClient_name() {
			return client_name;
		}



		public void setClient_name(String client_name) {
			this.client_name = client_name;
		}
		
		

	}

}
