package sh.okx.drainer;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class DrainerBot {
  public static String MSA_CLIENT_ID = "5f5dcb60-d121-4de2-9aec-4e1acbb2e6e2";

  private Properties properties;
  private File propertiesFile;

  private GameProfile profile;
  private String token;

  public DrainerBot() throws RequestException {
    setup();

    String authType = properties.getProperty("authType");
    if (authType.equals("mojang")) {
      MojangAuthenticationService auth = new MojangAuthenticationService(properties.getProperty("mojangClient"));
      auth.setUsername(properties.getProperty("mojangUsername"));
      auth.setAccessToken(properties.getProperty("mojangAuth"));
      auth.login();

      authMojang(auth);
    } else if (authType.equals("msa")) {
      MsaAuthenticationService auth = new MsaAuthenticationService(MSA_CLIENT_ID);
      auth.setUsername(properties.getProperty("msaUsername"));
      auth.setRefreshToken(properties.getProperty("msaRefresh"));
      auth.login();

      authMsa(auth);
    } else {
      throw new IllegalStateException("Invalid auth type");
    }
  }

  public DrainerBot(MojangAuthenticationService auth) {
    setup();
    authMojang(auth);
  }

  public DrainerBot(MsaAuthenticationService auth) {
    setup();
    authMsa(auth);
  }

  private void authMojang(MojangAuthenticationService auth) {

    properties.setProperty("authType", "mojang");
    properties.setProperty("mojangUsername", auth.getUsername());
    properties.setProperty("mojangAuth", auth.getAccessToken());
    properties.setProperty("mojangClient", auth.getClientToken());
    try (FileOutputStream os = new FileOutputStream(propertiesFile)) {
      properties.store(os, null);
    } catch (IOException e) {
      e.printStackTrace();
    }
    profile = auth.getSelectedProfile();
    token = auth.getAccessToken();

    login();
  }

  private void authMsa(MsaAuthenticationService auth) {
    properties.setProperty("authType", "msa");
    properties.setProperty("msaUsername", auth.getUsername());
    properties.setProperty("msaRefresh", auth.getRefreshToken());
    try (FileOutputStream os = new FileOutputStream(propertiesFile)) {
      properties.store(os, null);
    } catch (IOException e) {
      e.printStackTrace();
    }

    profile = auth.getSelectedProfile();
    token = auth.getAccessToken();

    login();
  }

  private void setup() {
    try {
      propertiesFile = new File("drainer.properties");
      propertiesFile.createNewFile();

      properties = new Properties();
      try (FileInputStream is = new FileInputStream(propertiesFile)) {
        properties.load(is);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void resetAuth() {
    properties.remove("authType");
    properties.remove("mojangUsername");
    properties.remove("mojangAuth");
    properties.remove("mojangClient");
    properties.remove("msaUsername");
    properties.remove("msaRefresh");
    try (FileOutputStream os = new FileOutputStream(propertiesFile)) {
      properties.store(os, null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void login() {
    new ConnectGui(this).setVisible(true);
  }

  public GameProfile getProfile() {
    return profile;
  }

  public Session loginTo(String host, int port, LoginCallback callback) {
    MinecraftProtocol protocol = new MinecraftProtocol(profile, token);
    Session client = new TcpClientSession(host, port, protocol);
    client.addListener(new SessionAdapter() {
      @Override
      public void connected(ConnectedEvent event) {
      }

      @Override
      public void packetReceived(PacketReceivedEvent event) {
        if(event.getPacket() instanceof ServerJoinGamePacket) {
          event.getSession().send(new ClientChatPacket("/streak"));
        } else if(event.getPacket() instanceof ServerChatPacket) {
          Component message = event.<ServerChatPacket>getPacket().getMessage();
          String plain = PlainTextComponentSerializer.plainText().serialize(message);
          System.out.println("Received Message: " + plain);
          String eligiblePrefix = "You will be eligible for daily rewards again in ";
          String timerPrefix = "You will receive your daily rewards in ";
          String receivedPrefix = "You've received your daily login reward";
          if (plain.startsWith(eligiblePrefix)) {
            callback.notEligible(client, plain.substring(eligiblePrefix.length()));
          } else if (plain.startsWith(timerPrefix)) {
            callback.receiveIn(client, plain.substring(timerPrefix.length()));
          } else if (plain.startsWith(receivedPrefix)) {
            callback.received(client);
          }
        }
      }

      @Override
      public void disconnected(DisconnectedEvent event) {
        System.out.println("Disconnected: " + event.getReason());
        if(event.getCause() != null) {
          event.getCause().printStackTrace();
        }
      }
    });

    client.connect(false);
    return client;
  }

  interface LoginCallback {
    void notEligible(Session client, String text);
    void receiveIn(Session client, String text);
    void received(Session client);
  }
}
