package com.asturcraft.defendTheVillage;

import com.chaseoes.forcerespawn.ForceRespawn;
import com.chaseoes.forcerespawn.event.ForceRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import net.minecraft.server.v1_7_R3.EntityLiving;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftZombie;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class Main extends JavaPlugin implements Listener {
	public LeerConfiguracion leerConfig = null;
	public static Main plugin = null;
	public ArenaManager am = null;

	public ItemStack libro_kits_objeto = new ItemStack(Material.ENCHANTED_BOOK);
	ItemMeta book1meta = this.libro_kits_objeto.getItemMeta();
	ArrayList<String> book1description = new ArrayList();
	public ItemStack esmeralda_item = new ItemStack(Material.EMERALD);
	ItemMeta esmeralda_item_meta = this.esmeralda_item.getItemMeta();
	ArrayList<String> emdescription = new ArrayList();
	ArrayList<String> allowedCommands = new ArrayList();
	
	//menu de los kits
	public static SeleccionarKit selecKit;
	
	//menu de los objetos
	public TiendaPuntos tiendaPuntos;

	public void onEnable() {
		plugin = this;
		_log("Enabling...");
	
		Bukkit.getPluginManager().registerEvents(this, this);
		this.leerConfig = new LeerConfiguracion(this);
		this.am = new ArenaManager(this);
		

		
		

		this.leerConfig.cu();
		if (this.leerConfig.cu)
			_log("leerConfig: OK");
		
		//libro de los kits
		this.book1meta.setDisplayName(this.leerConfig.get("kit_book"));
		this.book1description.add(ChatColor.DARK_GREEN + this.leerConfig.get("kit_book_desc"));
		this.book1meta.setLore(this.book1description);
		this.libro_kits_objeto.setItemMeta(this.book1meta);
		
		
		//tienda esmeralda
		this.esmeralda_item_meta.setDisplayName(this.leerConfig.get("emerald_shop"));
		this.emdescription.add(ChatColor.DARK_GREEN + this.leerConfig.get("emerald_shop_desc"));
		this.esmeralda_item_meta.setLore(this.emdescription);
		this.esmeralda_item.setItemMeta(this.esmeralda_item_meta);

		if (!new File(getDataFolder(), "config.yml").exists())
			saveResource("config.yml", false);
		else {
			LoadarenaConfig();
		}
		
	    //Para poder seleccionar los kits
	    selecKit = new SeleccionarKit(this);
	    
	    //Para poder seleccionar la tienda
	    tiendaPuntos = new TiendaPuntos(this);
		
		_logE("DEBUG INFO IS ENABLED!");

		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for (Arena a : Main.this.am.arenas)
					if (a.noHaEmpezado) {
						if (a.contador == 0) {
							Main.this.am.start(a);
							a.noHaEmpezado = false;
						} else {
							a.contador -= 1;
							if (a.contador % 5 == 0) {
								//Cada 5 segundos aviso a los jugadores
								for (String s : a.jugadores)
									Main.this.s(Bukkit.getPlayer(s), Main.plugin.leerConfig.get("starting_in").replace("$1", Integer.toString(a.contador)));
							}
						}
					}
					else if (a.esperandoSiguienteOleada)
						if (a.contador == 0) {
							//if (a.oleada != 1) { //Esto es para que la oleada 1 no la mate instantaneamente
								Main.this.am.nextwave(a);
								a.esperandoSiguienteOleada = false;
							//} else {
								//a.oleada += 1;
								//a.esperandoSiguienteOleada = false;
								//Main.this.am.nextwavePrimeraOleada(a);
							//}
						} else {
							a.contador -= 1; //restamos 1 al contador
						}
			}
		}, 20L, 20L);
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for (Arena a : Main.this.am.arenas)
					if ((!a.noHaEmpezado) && (!a.esperandoSiguienteOleada)) {
						boolean b = false;
						for (Entity e : a.z1.getWorld().getEntities()) {
							if ((e instanceof Zombie)) {
								b = true;
							}
						}
						if (!b)
							a.zombies.clear();
						Main.this.am.checkZombies(a);
					}
				Entity e;
				for (Arena a : Main.this.am.arenas) {
					if ((!a.noHaEmpezado) && (!a.esperandoSiguienteOleada)) {
						boolean b = false;
						Iterator itr = a.v1.getWorld().getEntities().iterator();
						while (itr.hasNext()) {
							e = (Entity) itr.next();
							if ((e instanceof Villager)) {
								b = true;
							}
						}
						if (!b)
							a.aldeanos.clear();
						Main.this.am.checkVillagers(a);
					}
				}
				for (Arena a : Main.this.am.arenas)
					if ((!a.noHaEmpezado) && (!a.esperandoSiguienteOleada)) {
						for (String s : a.getPlayers()) {
							Player p = Bukkit.getPlayer(s);
							if (p == null)
								a.getPlayers().remove(s);
						}
						Main.this.am.checkPlayers(a);
					}
			}
		}, 200L, 200L);
	}
	
	//FIN DEL onEnable()

	private void LoadarenaConfig()
	{
		if ((this.leerConfig.cu) && (getConfig().getList("config.allowed_commands") != null)) {
			this.allowedCommands = ((ArrayList)getConfig().getList("config.allowed_commands"));
		//Carga una lista de comandos...
		}
		if (getConfig().getConfigurationSection("arenas") != null) {
			Iterator iterator = getConfig().getConfigurationSection("arenas").getKeys(false).iterator();
			while (iterator.hasNext()) {
				String arena = (String)iterator.next();
				int id = 0;
				try {
					id = Integer.parseInt(arena);
				} catch (Exception localException) { }
				String name = getConfig().getString("arenas." + id + ".name");
				if (this.am.deserializeLoc(getConfig().getString("arenas." + id + ".ps")).getWorld() == null) {
					Bukkit.createWorld(new WorldCreator(getConfig().getString("arenas." + id + ".ps").split(",")[0]));
					_log("! World " + getConfig().getString(new StringBuilder("arenas.").append(id).append(".ps").toString()).split(",")[0] + "not found. Importing!");
				}

				Location z1 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".z1"));
				Location z2 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".z2"));
				Location z3 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".z3"));
				Location v1 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".v1"));
				Location v2 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".v2"));
				Location v3 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".v3"));
				Location ps = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".ps"));
				Location lobby = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".lobby"));
				int mp = getConfig().getInt("arenas." + id + ".maxplayers");
				Arena arenaname = new Arena(z1, z2, z3, v1, v2, v3, ps, lobby, name, id, mp);
				this.am.arenas.add(arenaname);

				if (getConfig().contains("arenas." + id + ".sign")) {
					Location sign = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".sign"));
					arenaname.sign = sign;
					updateSign(arenaname);
				}

				_log("! If you see any errors above, check arena: " + arena + " !");
			}
		}
		try {
			getConfig().save(getDataFolder() + System.getProperty("file.separator") + "config.yml");
		} catch (IOException e) {
			e.printStackTrace();
		}
	//	this.am.licencia();
	}
	
	//FIN del cargar configuraci�n de la arena

	public synchronized void reloadArena(final int id) {
		_log("Reloading arena " + id + ".");
		if ((this.leerConfig.cu) && (this.am.arenas.contains(this.am.getArena(id)))) {
			Arena a = this.am.getArena(id);
			for (String s : a.jugadoresmuertos) {
				Player p = Bukkit.getPlayer(s);
				p.setFlying(false);
				p.setAllowFlight(false);
				//p.teleport(a.ps);

				for (Player pl : Bukkit.getOnlinePlayers()) {
					if (p != pl) {
						pl.showPlayer(p);
					}
				}
				a.jugadoresmuertos.remove(s);
			}

			for (Zombie s : this.am.getArena(id).zombies) {
				s.remove();
			}
			for (Villager s : this.am.getArena(id).aldeanos) {
				s.remove();
			}
			for (Entity e : a.ps.getWorld().getEntities()) {
				if ((e.getType().equals(EntityType.ZOMBIE)) || (e.getType().equals(EntityType.VILLAGER))) {
					e.remove();
				}
			}
			for (String s : this.am.getArena(id).getPlayers()) {
				Player p = Bukkit.getPlayer(s);
				setKit(p, "tanque");
				this.am.removePlayer(p);
				//p.setHealth(20);
				//p.setFoodLevel(20);
			}
			
			a.check = false;

			//this.am.arenas.remove(this.am.getArena(id));
			Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable() {
				public void run() {
					am.arenas.remove(am.getArena(id));
				}
			}, 20L);
		}

		if (getConfig().getConfigurationSection("arenas." + id) != null) {
			String name = getConfig().getString("arenas." + id + ".name");
			Location z1 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".z1"));
			Location z2 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".z2"));
			Location z3 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".z3"));
			Location v1 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".v1"));
			Location v2 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".v2"));
			Location v3 = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".v3"));
			Location ps = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".ps"));
			Location lobby = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".lobby"));
			int mp = getConfig().getInt("arenas." + id + ".maxplayers");
			Arena arenaname = new Arena(z1, z2, z3, v1, v2, v3, ps, lobby, name, id, mp);
			this.am.arenas.add(arenaname);

			if (getConfig().contains("arenas." + id + ".sign")) {
				Location sign = this.am.deserializeLoc(getConfig().getString("arenas." + id + ".sign"));
				arenaname.sign = sign;
				updateSign(arenaname);
			}
			_log("! If you see any errors above, check arena: " + id + " !");
		}
	}
	//FIN de reloadArena

	public void onDisable() {
		for (Arena a : this.am.arenas)
			reloadArena(a.getId());
	}

	public String arrayToString(String[] args, int offset)		{
		String s = "";
		for (int i = 0; i <= args.length; i++) {
			if (i > offset) {
				s = s + args[(i - 1)] + " ";
			}
		}
		return s;
	}

	public boolean checkArgs(String s) {
		if ((s.equalsIgnoreCase("z1")) || (s.equalsIgnoreCase("z2")) || (s.equalsIgnoreCase("z3")) || (s.equalsIgnoreCase("v1")) || 
				(s.equalsIgnoreCase("v2")) || (s.equalsIgnoreCase("v3")) || (s.equalsIgnoreCase("name")) || 
				(s.equalsIgnoreCase("ps")) || (s.equalsIgnoreCase("lobby")) || (s.equalsIgnoreCase("maxplayers")))
			return true;
		return false;
	}

	public boolean checkArgsInt(String s) {
		if ((s.equalsIgnoreCase("maxplayers")))
			return true;
		return false;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if ((cmd.getName().equalsIgnoreCase("abandonar")) && (this.leerConfig.cu)) {
			if ((sender instanceof Player)) {
				Player pl = (Player)sender;
				if (this.am.isInGame(pl)) {
					this.am.removePlayer(pl);
					s(sender, this.leerConfig.get("left"));
				} else {
					s(sender, this.leerConfig.get("need_to_play"));
				}
			} else {
				s(sender, "Este comando solo lo puede ejecutar un jugador.");
			}
		}

		if ((cmd.getName().equalsIgnoreCase("vd")) && (this.leerConfig.cu)) {
			if (sender.hasPermission("vd.command")) {
				if (args.length > 1) {
					
					if (args[0].equalsIgnoreCase("crear")) {
						if ((sender instanceof Player)) {
							int id = 0;
							try {
								id = Integer.parseInt(args[1]);
							} catch (Exception e) {
								s(sender, "La ID debe ser un numero.");
								return false;
							}
							if ((id < 0) && (id > 26)) {
								s(sender, "Por favor elige una ID de 0 a 26.");
								return false;
							}
							if (isArena(id)) {
								s(sender, "Esa ID ya esta en uso.");
								return false;
							}
							Player p = (Player)sender;
							this.am.createArena(p.getLocation(), p.getLocation(), p.getLocation(), p.getLocation(), p.getLocation(), p.getLocation(), p.getLocation(), p.getLocation(), id + "_arena", id, 1, 10);
							s(p, "Arena numero" + id + " creada correctamente. Configurala con /vd configurar.");
						} else {
							s(sender, "Este comando solo lo puede ejecutar un jugador.");
						}
					}
					else if (args[0].equalsIgnoreCase("configurar")) {
						if (args.length < 3) {
							s(sender, "Uso: /vd configurar <id_de_la_arena> <configuracion> [argumentos]");
							return false;
						}
						if (getConfig().getConfigurationSection("arenas." + args[1]) == null) {
							s(sender, "ID de arena no encontrada.");
							return false;
						}
						int id = 0;
						try {
							id = Integer.parseInt(args[1]);
						} catch (Exception e) {
							s(sender, "La ID debe ser un numero.");
							return false;
						}
						if (checkArgs(args[2])) {
							if ((args[2].toLowerCase().startsWith("v")) || (args[2].toLowerCase().startsWith("z")) || (args[2].equalsIgnoreCase("ps")) || (args[2].equalsIgnoreCase("lobby"))) {
								if ((sender instanceof Player)) {
									Player p = (Player)sender;
									this.am.setArenaSetup(id, args[2], this.am.serializeLoc(p.getLocation()));
									reloadArena(id);
									s(sender, "Cambiada correctamente la configuracion de \"" + args[2] + "\" a \"" + this.am.serializeLoc(p.getLocation()) + "\".");
								} else {
									s(sender, "Este comando solo lo puede ejecutar un jugador.");
								}
							} else {
								if (args.length < 4) {
									s(sender, "Esta configuracion requiere argumentos.");
									return false;
								}
								if (checkArgsInt(args[2])) {
									try {
										int idas = Integer.parseInt(args[3]);
										this.am.setArenaSetup(id, args[2], Integer.valueOf(idas));
										reloadArena(id);
										s(sender, "Cambiada correctamente la configuracion de \"" + args[2] + "\" a \"" + idas + "\".");
										return false;
									} catch (Exception e) {
										s(sender, "La ID debe ser un numero.");
										return false;
									}
								}
								this.am.setArenaSetup(id, args[2], args[3]);
								reloadArena(id);
								s(sender, "Cambiada correctamente la configuracion de \"" + args[2] + "\" a \"" + args[3] + "\".");
							}
						}
						else s(sender, "Revisa tu configuracion.");
					}
				}
				else if (args.length == 1) {
					if (args[0].equalsIgnoreCase("crear")) {
						s(sender, "Crea tu arena con el comando: /vd crear <id_de_la_arena>");
						s(sender, "La ID de la arena debe ser de 0 a 26.");
						s(sender, "Despues configurala con: /vd configurar");
					} else if (args[0].equalsIgnoreCase("configurar")) {
						s(sender, "Configura tu arena con el comando: /vd configurar <id_de_la_arena> <configuracion> [argumentos]");
						s(sender, "Ejemplo: /vd configurar 1 name PrimeraArena");
						s(sender, "Configuraciones:");
						s(sender, "z1 ... z3 - configura los 3 puntos de spawn de zombies. Se configura en tu posicion actual.");
						s(sender, "v1 ... v3 - configura los 3 puntos de spawn de aldeanos. Se configura en tu posicion actual.");
						s(sender, "ps - configura el punto de spawn de los jugadores. Se configura en tu posicion actual.");
						s(sender, "lobby - configura el punto de spawn de los jugadores muertos. Debe de estar algo alejado. Yo recomiendo un sitio algo ;).");
						s(sender, "name - configura el nombre de la arena. Necesita el argumento: <nombre>");
						s(sender, "maxplayers - configura el numero maximo de jugadores. Necesita el argumento: <jugadores_maximo(solo numeros)>");
					} else {
						s(sender, "Prueba /vd.");
					}
				} else {
					s(sender, "Bienvenidos a Defiende la Villa.");
					s(sender, "Solo los jugadores con el permiso \"vd.command\" pueden usar este comando.");
					s(sender, "Comandos disponibles:");
					s(sender, "/vd crear - escribelo para mas informacion.");
					s(sender, "/vd configurar - escribelo para mas informacion.");
				}
			}
			else s(sender, this.leerConfig.get("no_perm"));
		}
		return false;
	} //FIN del onCommand

		
	//LOS M�TODOS DE ENVIAR LOS ERRORES ETC
	void s(CommandSender s, String ss) {
		s.sendMessage(ChatColor.GRAY + "V" + ChatColor.RED + "D " + ChatColor.WHITE + ss);
	}

	public static void _log(String s) {
		Bukkit.getLogger().info("[" + plugin.getDescription().getName() + "] " + s);
	}

	public static void _logE(String s) {
		Bukkit.getLogger().log(Level.SEVERE, "[" + plugin.getDescription().getName() + "] " + s);
	}

	public boolean isArena(int id) {
		for (Arena a : this.am.arenas)
			if (a.id == id)
				return true;
		return false;
	}

	//Este m�todo pone metadatos al jugador a la hora de elegir kit!!!!
	public void setKit(Player jugador, String s) {
		jugador.setMetadata("vdkit", new FixedMetadataValue(this, s));
	}

	//este m�todo lee los metadatos del jugador para el kit
	public String getKit(Player pl) {
		return ((MetadataValue)pl.getMetadata("vdkit").get(0)).asString();
	}

	//Para actualizar los carteles
	public void updateSign(Arena a) {
		if (a.sign != null) {
			Sign s = (Sign)a.sign.getBlock().getState();
			String name = a.pav;
			if (name.length() > 16)
				name = name.substring(0, 16);
			s.setLine(0, ChatColor.DARK_RED + a.pav);
			s.setLine(2, ChatColor.GREEN + "" + a.jugadores.size() + ChatColor.GREEN + "/" + ChatColor.GREEN + a.maximoJugadores);
			if (a.puedeUnirse) {
				s.setLine(1,ChatColor.DARK_PURPLE +  this.leerConfig.get("sb_starting"));
				s.setLine(3,ChatColor.BLUE + this.leerConfig.get("sign"));
			} else {
				s.setLine(1,ChatColor.DARK_PURPLE + this.leerConfig.get("sb_wave").replace("$1", Integer.toString(a.oleada)));
				s.setLine(3,ChatColor.DARK_BLUE + this.leerConfig.get("sign_full"));
			}
			s.update();
		}
	}

	@EventHandler
	public void onChange(SignChangeEvent ev) {
		if (ev.getLine(0).equals("[vd]")) {
			int aId = 0;
			try {
				aId = Integer.parseInt(ev.getLine(1));
			} catch (Exception e) {
				s(ev.getPlayer(), "La ID no es un numero.");
				return;
			}
			if (isArena(aId)) {
				this.am.setArenaSetup(aId, "sign", this.am.serializeLoc(ev.getBlock().getLocation()));
				reloadArena(aId);
				s(ev.getPlayer(), "Configurado correctamente \"sign\" a \"" + this.am.serializeLoc(ev.getBlock().getLocation()) + "\".");
			} else {
				s(ev.getPlayer(), "No existe ninguna arena con esa ID.");
			}
		}
	}

	@EventHandler
	public void onD(EntityDeathEvent ev) {
		
		//Si se muere un aldeano...
		if (ev.getEntityType().equals(EntityType.VILLAGER)) {
			Villager v = (Villager)ev.getEntity();
			for (Arena a : this.am.arenas) {
				if (a.aldeanos.contains(v)) {
					a.aldeanos.remove(v);
					this.am.updateSc(a);
					if (a.esperandoSiguienteOleada)
						return;
					for (String s : a.jugadores)
						s(Bukkit.getPlayer(s), this.leerConfig.get("vill_death").replace("$1", Integer.toString(a.aldeanos.size())));
					this.am.checkVillagers(a);
				}
			}
		}

		//Si lo que se muere es un zombie
		if (ev.getEntityType().equals(EntityType.ZOMBIE)) {
			ev.getDrops().clear(); //no dropea nada
			ev.setDroppedExp(0); //Ni experiencia
			
			//Ahora a�ado que dropee una gema!
			ItemStack gema = new ItemStack(Material.EMERALD, 1);
			ev.getDrops().add(gema);
			
			Zombie z = (Zombie)ev.getEntity();
			for (Arena a : this.am.arenas)
				if (a.zombies.contains(z)) {
					a.zombies.remove(z);
					this.am.updateSc(a);
					if (a.esperandoSiguienteOleada)
						return;
					this.am.checkZombies(a);

					//Vamos a buscar el que lo mat� para darle puntuaci�n
					if ((ev.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
						EntityDamageByEntityEvent nev = (EntityDamageByEntityEvent)ev.getEntity().getLastDamageCause();
						if ((nev.getDamager() instanceof Player)) {
							Player p = (Player)nev.getDamager();
							//Resulta que si es VIP se le da m�s puntos que no siendo vip... Estudiar a ver que se hace
							if (p.hasPermission("vd.vip")) {
								int ran = new Random().nextInt(15) + 16;
								this.am.addScore(p, ran);
								s(p, this.leerConfig.get("kill").replace("$1", Integer.toString(ran)));
							} else {
								int ran = new Random().nextInt(15) + 1;
								this.am.addScore(p, ran);
								s(p, this.leerConfig.get("kill").replace("$1", Integer.toString(ran)));
							}
						}
					}
				}
		}
	}
	
	//Evento para al coger la gema te de los puntos
	@EventHandler
	public void cogerGema(PlayerPickupItemEvent ev){
		 Player player = ev.getPlayer();
		 
		 boolean jugando = false;
		 
		 for (Arena a: this.am.arenas) {
			 if (a.jugadores.contains(player.getName())){
				 jugando = true;
				 break;
			 }
		 }
			
		 if (!jugando){
			 ev.setCancelled(true);
			 return;
		 }
		 
		 
		 for (Arena arena : this.am.arenas) { //Si el jugador est� muerto cancelo el evento, no puede coger nada
			if (arena.getPlayers().contains(player.getName())) {
				if (arena.jugadoresmuertos.contains(player.getName())) {
					ev.setCancelled(true);
					return;
				}
			}
		 } //FIN FOR
		 if (ev.getItem().getItemStack().getType() == Material.EMERALD) { //Si coge una esmeralda le vamos a dar 10 Gemas
			 Integer cantidad = ev.getItem().getItemStack().getAmount();
			 this.am.addScore(player, 10*cantidad); //10 puntos para el jugador
			 player.playSound(player.getLocation(), Sound.NOTE_PLING, 100.9F, 100.9F);
			 if (cantidad > 1) {
				 s(player, "Has recogido varias gemas. +" + 10*cantidad + " Gemas");
				 player.playSound(player.getLocation(), Sound.NOTE_PLING, 100.9F, 100.9F);
			 }
			 else {
				 s(player, "Has recogido una gema. +" + 10*cantidad + " Gemas");
				 player.playSound(player.getLocation(), Sound.NOTE_PLING, 100.9F, 100.9F);
			 }
			 player.playSound(player.getLocation(), Sound.NOTE_PLING, 100.9F, 100.9F);
			 ev.getItem().remove();
			 ev.setCancelled(true);
		 }
	}
	

	//Evento de si muere un jugador
	@EventHandler
	public void onDeath(PlayerDeathEvent ev) {
		if (this.am.isInGame(ev.getEntity())) {
			ev.getDrops().clear();
			ev.setDeathMessage(null);
			for (Arena a : this.am.arenas) {
				if (a.jugadores.contains(ev.getEntity().getName())) {
					for (Zombie z : a.zombies) {
						if (z.getTarget().equals(ev.getEntity())) {
							CraftZombie z2 = (CraftZombie) z;
							z2.getHandle().setGoalTarget(((CraftLivingEntity) a.aldeanos.get(0)).getHandle());
						}
					}
				}
			}
		}
	}

	//Evento de forcerespawn... se podr� eliminar o hacer de alguna otra forma?
	@EventHandler
	public void onForceRespawn(ForceRespawnEvent ev) {
		if (this.am.isInGame(ev.getPlayer())) {
			ev.setForcedRespawn(true);
			for (Arena a : this.am.arenas)
				if (a.jugadores.contains(ev.getPlayer().getName())) {
					if ((!a.esperandoSiguienteOleada) && (!a.noHaEmpezado) && (!a.jugadoresmuertos.contains(ev.getPlayer().getName()))) {
						a.jugadoresmuertos.add(ev.getPlayer().getName());
						for (Player p : Bukkit.getOnlinePlayers()) {
							if (p != ev.getPlayer()) {
								p.hidePlayer(ev.getPlayer());
							}
						}

						ev.getPlayer().setAllowFlight(true);
						ev.getPlayer().setFlying(true);
						for (String s : a.getPlayers()) {
							s(Bukkit.getPlayer(s), this.leerConfig.get("died").replace("$1", ev.getPlayer().getName()));
						}
						
						for(PotionEffect pe : ev.getPlayer().getActivePotionEffects()){
							ev.getPlayer().removePotionEffect(pe.getType());
						}
						
						this.am.updateSc(a);
						
					}
					this.am.checkPlayers(a);
				}
		}
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent ev)
	{
		if (this.am.isInGame(ev.getPlayer())) {
			for (Arena a : this.am.arenas) {
				if (a.jugadores.contains(ev.getPlayer().getName())) {
					if ((!a.esperandoSiguienteOleada) && (!a.noHaEmpezado)) {
						//ev.setRespawnLocation(ev.getPlayer().getLocation());
						ev.setRespawnLocation(a.lobby);
					} else {
						
						
						Player p = ev.getPlayer();
						this.am.a�adirKit(p);
						ev.setRespawnLocation(a.ps);
					}
				}
			}
		}
	}
	//FIN DEL RESPAWN de dar los items!

	//Evento de que sale el jugador
	@EventHandler
	public void onLeave(PlayerQuitEvent ev) {
		if (this.am.isInGame(ev.getPlayer())) {
			this.am.removePlayer(ev.getPlayer());
		}
	}

	//Evento de que se une al servidor
	@EventHandler
	public void onJoin(PlayerJoinEvent ev) {
		if (ev.getPlayer().isDead())
			ForceRespawn.sendRespawnPacket(ev.getPlayer());
		ev.getPlayer().getInventory().clear();
		setKit(ev.getPlayer(), "tanque"); //Pone el kit default de tank a todo el que se una al servidor
	}

	//Evento de dropear objetos
	@EventHandler
	public void onDrop(PlayerDropItemEvent ev) {
		Material m = ev.getItemDrop().getItemStack().getType();
		if ((!m.equals(Material.COOKED_BEEF)) && (!m.equals(Material.APPLE)) && (!m.equals(Material.ROTTEN_FLESH)))
			ev.setCancelled(true);
	}

	//Esto anula comandos salvo los que est�n en la config de no_command_in_game
	@EventHandler
	public void onCommandPre(PlayerCommandPreprocessEvent ev) {
		if (this.am.isInGame(ev.getPlayer())) {
			boolean c = true;
			for (String com : this.allowedCommands) {
				if (ev.getMessage().toLowerCase().startsWith("/" + com)) {
					c = false;
				}
			}
			if (c)
				s(ev.getPlayer(), this.leerConfig.get("no_command_in_game"));
			ev.setCancelled(c);
		}
	}

	//Evento del target de los zombies
	@EventHandler
	public void onTarget(EntityTargetEvent ev) { //EntityTargetLivingEntityEvent ev) {
		if ((ev.getTarget() instanceof Player)) {
			Player pl = (Player)ev.getTarget();
			if (this.am.isInGame(pl))
				for (Arena a : this.am.arenas)
					if ((a.jugadoresmuertos.contains(pl.getName())) || (a.noHaEmpezado) || (a.esperandoSiguienteOleada)) {
						ev.setCancelled(true);
					}
		}
	}

	//Evento para cancelar el hambre si est� jugando
	@EventHandler
	public void onHunger(FoodLevelChangeEvent ev) {
		if ((ev.getEntity() instanceof Player)) {
			Player pl = (Player)ev.getEntity();
			if (this.am.isInGame(pl)) {
				for (Arena a : this.am.arenas) {
					if (a.jugadoresmuertos.contains(pl.getName()))
						ev.setCancelled(true);
				}
			}
			else
				ev.setCancelled(true);
		}
	}

	//Evento de da�o a las entidades
	@EventHandler
	public void entityDamage(EntityDamageEvent ev) {
		if ((ev.getEntity() instanceof Player)) {
			Player pl = (Player)ev.getEntity();
			if (this.am.isInGame(pl)) {
				for (Arena a : this.am.arenas) {
					if ((a.jugadoresmuertos.contains(pl.getName())) || (a.noHaEmpezado)) {
						ev.setCancelled(true);
					}
				}
			}
			else if (ev.getCause().equals(EntityDamageEvent.DamageCause.VOID)) {
				ev.setCancelled(true);
				pl.teleport(pl.getWorld().getSpawnLocation());
			}
		}
		else if ((((ev.getEntity() instanceof Player)) || ((ev.getEntity() instanceof Villager))) && ((ev.getCause().equals(EntityDamageEvent.DamageCause.FIRE)) || (ev.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)) || (ev.getCause().equals(EntityDamageEvent.DamageCause.POISON)) || (ev.getCause().equals(EntityDamageEvent.DamageCause.WITHER)) || (ev.getCause().equals(EntityDamageEvent.DamageCause.MAGIC)))) {
			ev.getEntity().setFireTicks(0);
			ev.setCancelled(true);
		}
	}

	//Para evitar que ardan al sol
	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		if ((event.getEntity() instanceof Zombie))
			event.setCancelled(true);
	}

	//Para evitar da�ar a los aldeanos u a otros jugadores
	@EventHandler
	public void onDamage(EntityDamageByEntityEvent ev) {
		if ((ev.getDamager() instanceof Player)) {
			Player pl = (Player)ev.getDamager();
			if (this.am.isInGame(pl)) {
				if (((ev.getEntity() instanceof Villager)) || ((ev.getEntity() instanceof Player))) {
					ev.setCancelled(true);
				}
				for (Arena a : this.am.arenas)
					if (a.jugadoresmuertos.contains(pl.getName()))
						ev.setCancelled(true);
			}
		}
		else if (ev.getDamager() instanceof Arrow){ //Para evitar el da�o por arco
			if (((ev.getEntity() instanceof Villager)) || ((ev.getEntity() instanceof Player))) {
				ev.setCancelled(true);
			}
        }
	}

	//Para que no puedan comerciar con los aldeanos
	@EventHandler
	public void onInterEnt(PlayerInteractEntityEvent ev) {
		if (((ev.getRightClicked() instanceof Villager)) && (this.am.isInGame(ev.getPlayer())))
			ev.setCancelled(true);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent ev) {
		//Object localObject1;
		
		Player jugador = ev.getPlayer();
		
		if (this.am.isInGame(ev.getPlayer()))
		{
			Iterator<Arena> localObject1 = this.am.arenas.iterator();
			while (localObject1.hasNext()) {
				Arena a = localObject1.next();
				if (a.jugadoresmuertos.contains(ev.getPlayer().getName())) {
					ev.setCancelled(true);
				}
			}
		}
		
		Boolean clickSign = false;

		if (ev.getClickedBlock() != null) {
			Iterator<Arena> localObject2 = this.am.arenas.iterator();
			while (localObject2.hasNext()) {
				Arena a = localObject2.next();
				if ((a.sign != null) && (a.sign.equals(ev.getClickedBlock().getLocation()))) {
					this.am.addPlayer(ev.getPlayer(), a.id);
					clickSign = true;
					ev.setCancelled(true);
				}
			}
		}

		if ((jugador.getItemInHand().equals(this.libro_kits_objeto) && (!clickSign))) {
			selecKit.show(jugador);
			ev.setCancelled(true);
		}
		
		if ((jugador.getItemInHand().equals(this.esmeralda_item) && (!clickSign))) {
			tiendaPuntos.show(jugador);
			ev.setCancelled(true);
		}
	}
	
	//Evento los libros!!!!
	@EventHandler
	public void onInventoryClickEvent (InventoryClickEvent event) {
		//Bloqueo los items ligados en todos los contenedores
		Player jugador = (Player) event.getWhoClicked(); //Lo parseamos
	
		if ((event.getInventory().getType() == InventoryType.CHEST) && (event.getInventory().getName().equals("Selecciona tu kit"))){
			
			int slot = event.getRawSlot();
			
			if ((slot > 21) || (slot == -999)) {
				event.setCancelled(true);
				return;
			}
			
			if (event.getCurrentItem().getItemMeta() == null) return;
			
			//Metodo para evitar que cambien de kit en mitad de una partida
			for (Arena a: this.am.arenas) {
				if (a.jugadores.contains(jugador.getName())){
					if (jugador.hasPermission("vd.vip")) {
						//si el jugador es vip
						if (!a.cambiarKit) {
							s(jugador, "Solo puedes cambiar de kit en el descanso entre oleadas.");
							event.setCancelled(true);
							jugador.closeInventory();
							return;
						}
					}
					else {
						if (!a.noHaEmpezado) {
							event.setCancelled(true);
							jugador.closeInventory();
							return;
						}
					}
				}
			}

			if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Tanque")) {
				Main.this.setKit(jugador, "tanque");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Piromano")) {
				Main.this.setKit(jugador, "piromano");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Bruja")) {
				Main.this.setKit(jugador, "bruja");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Hardcore")) {
				Main.this.setKit(jugador, "hardcore");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Arquero")) {
				Main.this.setKit(jugador, "arquero");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Cadete")) {
				Main.this.setKit(jugador, "cadete");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Peleador")) {
				Main.this.setKit(jugador, "peleador");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Congelado")) {
				Main.this.setKit(jugador, "congelado");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Parkour")) {
				Main.this.setKit(jugador, "parkour");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Gordito")) {
				Main.this.setKit(jugador, "gordito");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Defensa")) {
				Main.this.setKit(jugador, "defensa");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Corredor")) {
				Main.this.setKit(jugador, "corredor");
                Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Espadachin")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "espadachin");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Pacifico")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "pacifico");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Protegido")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "protegido");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Experto")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "experto");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Sabueso")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "sabueso");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Conejo")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "conejo");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Enfermero")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "enfermero");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Legolas")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "legolas");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("OP")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "op");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Dorado")) {
				if (jugador.hasPermission("vd.vip")) {
					Main.this.setKit(jugador, "dorado");
					Main.this.s(jugador, Main.this.leerConfig.get("kit_selected"));
				} else {
					Main.this.s(jugador, Main.this.leerConfig.get("kit_vip"));
				}
				event.setCancelled(true);
				jugador.closeInventory();
			}
			
			
			
			
		}
		else if ((event.getInventory().getType() == InventoryType.CHEST) && (event.getInventory().getName().equals("Compra objetos"))){
			
			int slot = event.getRawSlot();
			
			if ((slot > 31) || (slot == -999)) {
				event.setCancelled(true);
				return;
			}
			
			if (event.getCurrentItem().getItemMeta() == null){
				event.setCancelled(true);
				return; //Si es aire, vuelve...
			}
			
			boolean jugando = false;
			
			for (Arena a: this.am.arenas) {
				if (a.jugadores.contains(jugador.getName())){
					jugando = true;
					break;
				}
			}
			
			if (!jugando){
				event.setCancelled(true);
				jugador.closeInventory();
				return;
			}

			Integer i = 0;
			
			List<ItemStack> copia = tiendaPuntos.listaObjetos;
			Iterator<ItemStack> itr = copia.iterator();
			while (itr.hasNext()) {
				if (i == slot) { //Si coinciden es mi objeto
					ItemStack objeto = itr.next();
					ItemMeta meta = (ItemMeta) objeto.getItemMeta();
					String valorString = meta.getLore().toString().replace(" puntos.", "").replace("[", "").replace("]", "");
					Integer valor = Integer.parseInt(valorString);
					Integer puntos = this.am.getScore(jugador);
					if (valor > puntos) {
						s(jugador, "No tienes puntos suficientes");
						event.setCancelled(true);
						jugador.closeInventory();
						break;
					}
					else {
						this.am.setScore(jugador, puntos-valor);
						jugador.getInventory().addItem(objeto);
						event.setCancelled(true);
						jugador.closeInventory();
						break;
					}
				}
				else {
					itr.next();
					i++;
				}
			}
		}
		
		else { //Otros inventarios
			if (event.getCurrentItem() == null) return;
			else if (event.getCurrentItem().getType() == Material.ENCHANTED_BOOK) {
				event.setCancelled(true);
				return;
			}
			else if (event.getCurrentItem().getType() == Material.EMERALD) {
				event.setCancelled(true);
				return;
			}
		}
	}
	
}