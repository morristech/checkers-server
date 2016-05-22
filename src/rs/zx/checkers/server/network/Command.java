package rs.zx.checkers.server.network;

import java.util.ArrayList;

import rs.zx.checkers.server.model.Game;
import rs.zx.checkers.server.model.Player;
import rs.zx.checkers.server.utils.Utils;

public enum Command {
	LOGIN(1) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			if(con.getPlayer() == null) {
				if(Server.availableName(arguments[0])) {
					Player p = new Player(Utils.randomId(6), arguments[0]);
	
					Server.assignConnection(p, con);
	
					con.sendMessage("E_OK");
					
					Server.broadcastUsers();
				} else {
					con.sendMessage("E_USERNAME_TAKEN");
				}
			} else {
				con.sendMessage("E_ALREADY_LOGGED_IN");
			}
		}			
	},
	GAME_REQUEST(1) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {	 	
			if(con.getPlayer() != null) {
				Player p = Server.getPlayer(arguments[0]);
				
				if(p != null) {
					Connection c = Server.getConnection(p);
					c.sendMessage("E_GAME_REQUEST: " + con.getPlayer().getName());
					
					c.addRequest(con.getPlayer());
					
					Command.valueOf("REQUESTS").run(c);
					
					con.sendMessage("E_OK");
				} else {
					con.sendMessage("E_NO_PLAYER");
				}
			} else {
				con.sendMessage("E_NO_PLAYER");
			}
		}
	},
	GAME_ACCEPT(1) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {	 	
			if(con.getPlayer() != null) {
				Player p = Server.getPlayer(arguments[0]);
				
				if(p != null && Server.freeUser(p)) {
					Connection c = Server.getConnection(p);
					
					String id = Utils.randomId(10);
					
					Game g = new Game(id);
				
					Server.newGame(id, g);
					
					g.joinGame(p);
					g.joinGame(con.getPlayer());
					
					Command.valueOf("REQUESTS").run(c);
					
					c.sendMessage("E_GAME_ACCEPTED: " + con.getPlayer().getName());
					con.sendMessage("E_OK: " + id);
					
					Server.broadcastGames();
				} else {
					con.sendMessage("E_NO_PLAYER");
				}
			} else {
				con.sendMessage("E_NO_PLAYER");
			}
		}
	},
	GAME_DECLINE(1) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {	 	
			if(con.getPlayer() != null) {
				Player p = Server.getPlayer(arguments[0]);
				
				if(p != null) {
					Connection c = Server.getConnection(p);
					
					Command.valueOf("REQUESTS").run(c);
					
					c.sendMessage("E_GAME_DECLINED: " + con.getPlayer().getName());
				} else {
					con.sendMessage("E_NO_PLAYER");
				}
			} else {
				con.sendMessage("E_NO_PLAYER");
			}
		}
	},
	REQUESTS(0) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {	 	
			if(con.getPlayer() != null) {
				con.sendMessage("E_REQUESTS:");
				for(Player p : con.getReqs())
					con.sendMessage(p.getName());
				con.sendMessage("E_END");
			} else {
				con.sendMessage("E_NO_PLAYER");
			}
		}
	},
	LEAVE_GAME(1) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {	 	
			if(con.getPlayer() != null) {
				Server.getGame(arguments[0]).leaveGame(con.getPlayer());
			
				con.sendMessage("E_OK");
				
				Server.broadcastGames();
			} else {
				con.sendMessage("E_NO_PLAYER");
			}
		}
	},
	GAMEMSG(1) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			if(con.getPlayer() != null) {
				Game g = Server.getPlayerGame(con.getPlayer());
				if(g != null) {
					Server.sendMessage(con.getPlayer(), g, arguments[0]);
					
					con.sendMessage("E_OK");
				} else {
					con.sendMessage("E_NO_GAME");
				}
			} else {
				con.sendMessage("E_NO_PLAYER");
			}
		}
	},
	LOBBYMSG(1) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			if(con.getPlayer() != null) {
				Server.sendMessage(con.getPlayer(), arguments[0]);
					
				con.sendMessage("E_OK");
			} else {
				con.sendMessage("E_NO_PLAYER");
			}
		}
	},
	MOVE(4) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			if(con.getPlayer() != null) {
				Game g = Server.getPlayerGame(con.getPlayer());
				Player p = con.getPlayer();
				 
				if(g != null) {
					if(g.getCurrentPlayer() == p) {						
						try {
							g.playMove(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]), Integer.parseInt(arguments[2]), Integer.parseInt(arguments[3]));
							
							Server.sendGameEvent(g, "E_MOVE", arguments);
							
							if(g.isOver())
								Server.sendMessage(null, g, "Game is over!");
						} catch(NumberFormatException e) {
							con.sendMessage("E_INVALID_ARGUMENTS");
						}

						con.sendMessage("E_OK");
					} else 
						con.sendMessage("E_INVALID_MOVE");					
				} else 
					con.sendMessage("E_NO_GAME");				
			} else
				con.sendMessage("E_NO_PLAYER");
		}
	},
	EAT(2) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			if(con.getPlayer() != null) {
				Game g = Server.getPlayerGame(con.getPlayer());
				Player p = con.getPlayer();
				 
				if(g != null) {
					if(g.getCurrentPlayer() == p) {						
						try {
							g.playEat(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]));
							
							Server.sendGameEvent(g, "E_EAT", arguments);
							
							if(g.isOver())
								Server.sendMessage(null, g, "Game is over!");
						} catch(NumberFormatException e) {
							con.sendMessage("E_INVALID_ARGUMENTS");
						}

						con.sendMessage("E_OK");
					} else 
						con.sendMessage("E_INVALID_MOVE");					
				} else 
					con.sendMessage("E_NO_GAME");				
			} else
				con.sendMessage("E_NO_PLAYER");
		}
	},
	END_TURN(0) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			if(con.getPlayer() != null) {
				Game g = Server.getPlayerGame(con.getPlayer());
				Player p = con.getPlayer();
				 
				if(g != null) {
					if(g.getCurrentPlayer() == p) {
						g.changePlayer();
						
						Server.sendGameEvent(g, "E_TURN", g.getCurrentPlayer().getName());

						con.sendMessage("E_OK");
					} else 
						con.sendMessage("E_INVALID_MOVE");					
				} else 
					con.sendMessage("E_NO_GAME");				
			} else
				con.sendMessage("E_NO_PLAYER");
		}
	},
	PONG(0) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			con.setLag((System.currentTimeMillis()-con.getLastPingTime())/1000.0);
			con.setAlive(true);
			con.sendMessage("E_LAG: " + con.getLag());
		} 
	},
	//for fun
	HELP(0) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			con.sendMessage("E_COMMANDS:\r\nLOGIN: <username>\r\nPONG\r\nGAME REQUEST: <username>\r\nGAME ACCEPT: <username>\r\nGAME DECLINE: <username>\r\nLEAVE GAME\r\nGAMEMSG: <message>\r\nLOBBYMSG: <message>\r\nMOVE: <from_x> <from_y> <to_x> <to_y> <eaten>\r\nEND TURN\r\nUSERS\r\nFREE USERS\r\nGAMES\r\nHELP");
		}		 
	},
	USERS(0) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			con.sendMessage("E_USERS:");
			Server.getPlayers().stream().forEach(i -> {
				con.sendMessage(i.getName());
			});
			con.sendMessage("E_END");
		}		 
	},
	FREE_USERS(0) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			con.sendMessage("E_USERS:");
			Server.getPlayers().stream().filter(i -> {
				return Server.getPlayerGame(i) == null;
			}).forEach(i -> {
				con.sendMessage(i.getName());
			});	
			con.sendMessage("E_END");
		}		 
	},
	GAMES(0) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			StringBuilder command = new StringBuilder("E_GAMES:\r\n");
			Server.getGames().stream().forEach(i -> {
				ArrayList<Player> p = i.getPlayers();
				command.append(p.get(0).getName() + " " + p.get(1).getName()+"\r\n");
			});
			command.append("E_END");
			
			con.sendMessage(command.toString());
		}		 
	},
	GAME_OVER(0) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			if(con.getPlayer() != null) {
				Game g = Server.getPlayerGame(con.getPlayer());
				 
				if(g != null) {
					g.setOver(true);				
				} else 
					con.sendMessage("E_NO_GAME");				
			} else
				con.sendMessage("E_NO_PLAYER");
		}		 
	},
	STATE(1) {
		@Override
		public void run(Connection con, String... arguments) throws Exception {
			if(con.getPlayer() != null) {
				Game g = Server.getGame(arguments[0]);
				 
				if(g != null) {
					con.sendMessage("E_STATE:");
					con.sendMessage(Utils.toString(g));
				} else 
					con.sendMessage("E_NO_GAME");				
			} else
				con.sendMessage("E_NO_PLAYER");
		}
	};

	private int argumentCount;

	private Command(int argumentCount) {
		this.argumentCount = argumentCount;
	}

	public int getArgumentCount() {
		return argumentCount;
	}

	public abstract void run(Connection con, String... arguments) throws Exception;
}
