package sh.okx.drainer;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import sh.okx.drainer.DrainerBot.LoginCallback;

public class ConnectGui extends JFrame {

  private final DrainerBot drainerBot;

  private JButton signOutButton;
  private JLabel loggedInAsLabel;

  private JLabel serverLabel;
  private JTextField serverField;

  private JButton connectButton;
  private JLabel connectStatus;

  private JTextPane statusPane;

  private JLabel countDown;

  private Session session;

  private int secondsLeft = 0;

  private TimerTask timer;

  public ConnectGui(DrainerBot drainerBot) {
    this.drainerBot = drainerBot;
    initComponents();
    setResizable(false);
    setLocationRelativeTo(null);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   */

  private void initComponents() {
    String[] options = {"Mojang", "Microsoft"};

    // Window setup
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setTitle("Drainer");

    // Component setup

    signOutButton = new JButton();
    signOutButton.setText("Sign out");

    loggedInAsLabel = new JLabel();
    loggedInAsLabel.setLabelFor(signOutButton);
    loggedInAsLabel.setText("Signed in as: " + drainerBot.getProfile().getName());

    serverLabel = new JLabel();
    serverLabel.setText("Server");

    serverField = new JTextField();
    serverLabel.setLabelFor(serverLabel);

    connectButton = new JButton();
    connectButton.setText("Connect");

    connectStatus = new JLabel();

    statusPane = new JTextPane();
    statusPane.setEditable(false);
    statusPane.setFocusable(false);
    statusPane.setPreferredSize(new Dimension(1, 48));

    countDown = new JLabel();
    countDown.setFont(new Font("Liberation Mono", Font.PLAIN, 30));
    updateCountDown();

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);

    SequentialGroup paddedHorizontal = layout.createSequentialGroup().addContainerGap();
    ParallelGroup horizontal = layout.createParallelGroup();
    SequentialGroup vertical = layout.createSequentialGroup();
    vertical.addContainerGap();

    horizontal.addGroup(layout.createSequentialGroup()
        .addComponent(signOutButton)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(loggedInAsLabel));

    horizontal.addGroup(layout.createSequentialGroup()
        .addComponent(serverLabel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(serverField));

    horizontal.addGroup(layout.createSequentialGroup()
        .addComponent(connectButton)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(connectStatus));

    horizontal.addGroup(layout.createSequentialGroup()
        .addComponent(statusPane));

    horizontal.addGroup(layout.createSequentialGroup()
        .addComponent(countDown));

    vertical.addGroup(layout.createParallelGroup()
        .addComponent(signOutButton)
        .addComponent(loggedInAsLabel, Alignment.CENTER));

    vertical.addPreferredGap(ComponentPlacement.UNRELATED);
    vertical.addGroup(layout.createParallelGroup()
        .addComponent(serverLabel, Alignment.CENTER)
        .addComponent(serverField));

    vertical.addPreferredGap(ComponentPlacement.RELATED);
    vertical.addGroup(layout.createParallelGroup()
        .addComponent(connectButton)
        .addComponent(connectStatus, Alignment.CENTER));

    vertical.addPreferredGap(ComponentPlacement.RELATED);
    vertical.addGroup(layout.createParallelGroup()
        .addComponent(statusPane));

    vertical.addPreferredGap(ComponentPlacement.RELATED);
    vertical.addGroup(layout.createSequentialGroup().addComponent(countDown));

    vertical.addContainerGap();
    paddedHorizontal.addGroup(horizontal).addContainerGap();
    layout.setVerticalGroup(vertical);
    layout.setHorizontalGroup(paddedHorizontal);

    addMouseListeners();

    pack();
  }

  private void updateCountDown() {
    int mins = secondsLeft / 60;
    int secs = secondsLeft % 60;
    DecimalFormat df = new DecimalFormat("00");
    countDown.setText(df.format(mins) + ":" + df.format(secs));
  }

  private void addMouseListeners() {
    signOutButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (session != null) {
          session.disconnect("Signed out");
        }
        drainerBot.resetAuth();
        setVisible(false);
        dispose();
        new LoginGui().setVisible(true);
      }
    });

    connectButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (!connectButton.isEnabled()) return;
        connectButton.setEnabled(false);

        if (connectButton.getText().equals("Disconnect")) {
          session.disconnect("Pressed disconnect button");
          connectButton.setText("Connect");
          statusPane.setText("Status: Manually disconnected");
          return;
        }

        statusPane.setText("");
        connectStatus.setText("Connecting...");

        String server = ConnectGui.this.serverField.getText();
        String[] hostAndPort = server.split(":", 2);
        String host = hostAndPort[0];
        int port = 25565;
        if (hostAndPort.length > 1) {
          try {
            port = Integer.parseInt(hostAndPort[1]);
          } catch (NumberFormatException ex) {
            host = server;
          }
        }
        String fhost = host;
        int fport = port;
        new Thread(() -> {
          session = drainerBot.loginTo(fhost, fport, new LoginCallback() {
            @Override
            public void notEligible(Session s, String time) {
              SwingUtilities.invokeLater(() -> {
                statusPane.setText("Status: Eligible in " + time);
                s.disconnect("Not eligible");
              });
            }

            @Override
            public void receiveIn(Session client, String text) {
              secondsLeft = (Integer.parseInt(text.split(" ")[0]) * 60) + 60;
              System.out.println("Adjusted secondsLeft to "+  secondsLeft);
              if (timer != null) timer.cancel();
              new Timer().scheduleAtFixedRate(timer = new TimerTask() {
                private int run = 0;
                @Override
                public void run() {
                  if (secondsLeft <= 0) {
                    SwingUtilities.invokeLater(() -> {
                      secondsLeft = 0;
                      updateCountDown();
                    });
                    cancel();
                    return;
                  } else if (run >= 2 && secondsLeft % 60 == 0) {
                    client.send(new ClientChatPacket("/streak"));
                    return;
                  }
                  run++;
                  secondsLeft--;
                  SwingUtilities.invokeLater(ConnectGui.this::updateCountDown);
                }
              }, 0, 1000);
              SwingUtilities.invokeLater(() -> {
                statusPane.setText("Status: Waiting for essence in " + text);
              });
            }

            @Override
            public void received(Session client) {
              if (timer != null) timer.cancel();
              SwingUtilities.invokeLater(() -> {
                secondsLeft = 0;
                updateCountDown();
                statusPane.setText("Status: Received streak rewards");
              });
              client.disconnect("Received streak rewards");
            }
          });
          session.addListener(new SessionAdapter() {
            @Override
            public void connected(ConnectedEvent event) {
              SwingUtilities.invokeLater(() -> {
                statusPane.setText("Status: Running /streak");
                connectButton.setText("Disconnect");
                connectButton.setEnabled(true);
                connectStatus.setText("Connected.");
              });
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
              SwingUtilities.invokeLater(() -> {
                secondsLeft = 0;
                updateCountDown();
                connectButton.setText("Connect");
                connectButton.setEnabled(true);
                connectStatus.setText("Disconnected");
              });
            }
          });
        }).start();
      }
    });
  }
}
