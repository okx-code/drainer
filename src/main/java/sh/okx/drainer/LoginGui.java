package sh.okx.drainer;

import com.github.steveice10.mc.auth.exception.request.AuthPendingException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import sh.okx.drainer.MsaAuthenticationService.MsCodeResponse;

public class LoginGui extends JFrame {

  private JLabel authenticationTypeLabel;
  private JComboBox<String> authenticationTypeBox;

  private JLabel mojangEmailLabel;
  private JTextField mojangEmailField;

  private JLabel mojangPasswordLabel;
  private JPasswordField mojangPasswordField;

  private JTextPane mojangError;

  private JButton mojangDone;

  private JTextPane msaPrompt;
  private JButton msaDone;

  private JTextPane msaError;

  private JPanel msaAuthType;
  private JPanel mojangAuthType;

  private MsaAuthenticationService msaAuth;
  private MsCodeResponse msaResponse;

  public LoginGui() {
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

    // First row
    authenticationTypeBox = new JComboBox<>(options);

    authenticationTypeBox.addActionListener(e -> redraw());

    authenticationTypeLabel = new JLabel();
    authenticationTypeLabel.setText("Select authentication type");

    // Second row - Mojang
    mojangEmailLabel = new JLabel();
    mojangEmailLabel.setText("Email");

    mojangEmailField = new JTextField();
    mojangEmailLabel.setLabelFor(mojangEmailField);

    // Third row - Mojang
    mojangPasswordLabel = new JLabel();
    mojangPasswordLabel.setText("Password");

    mojangPasswordField = new JPasswordField();

    // Fourth row - Mojang
    mojangError = new JTextPane();
    mojangError.setEditable(false);
    mojangError.setFocusable(false);
    mojangError.setForeground(Color.RED);

    // Last row - Mojang
    mojangDone = new JButton();
    mojangDone.setText("Done");

    mojangDone.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        mojangClick();
      }
    });
    mojangDone.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {

      }

      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          mojangClick();
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {

      }
    });

    // Second row - Microsoft
    msaPrompt = new JTextPane();
    msaPrompt.setEditable(false);
    msaPrompt.setText("Querying Microsoft services...");

    // Third row - Microsoft
    msaError = new JTextPane();
    msaError.setEditable(false);
    msaError.setForeground(Color.RED);

    // Left row - Microsoft
    msaDone = new JButton();
    msaDone.setText("Done");
    msaDone.setVisible(false);

    msaDone.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        msaClick();
      }
    });

    msaPrompt.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        Element ele = msaPrompt.getStyledDocument()
            .getCharacterElement(msaPrompt.viewToModel(e.getPoint()));
        AttributeSet as = ele.getAttributes();
        AbstractAction fla = (AbstractAction) as.getAttribute("linkact");
        if (fla != null) {
          fla.actionPerformed(null);
        }
      }
    });
    msaPrompt.addMouseMotionListener(new MouseMotionAdapter() {
      private boolean hover = false;

      @Override
      public void mouseMoved(MouseEvent e) {
        Element ele = msaPrompt.getStyledDocument()
            .getCharacterElement(msaPrompt.viewToModel(e.getPoint()));
        AttributeSet as = ele.getAttributes();
        AbstractAction fla = (AbstractAction) as.getAttribute("linkact");
        if (fla != null) {
          LoginGui.this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          hover = true;
        } else if (hover) {
          LoginGui.this.setCursor(Cursor.getDefaultCursor());
          hover = false;
        }
      }
    });

    mojangSetup();
    msaSetup();

    redraw();
    pack();
  }

  private void redraw() {
    getContentPane().removeAll();
    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);

    // Horizontal
    ParallelGroup horizontal = layout.createParallelGroup();

    SequentialGroup firstRowColumns = layout.createSequentialGroup();
    firstRowColumns.addContainerGap();
    firstRowColumns.addComponent(authenticationTypeLabel);
    firstRowColumns.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
    firstRowColumns.addComponent(authenticationTypeBox, GroupLayout.DEFAULT_SIZE,
        GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
    firstRowColumns.addContainerGap();
    horizontal.addGroup(firstRowColumns);

    // Vertical
    SequentialGroup vertical = layout.createSequentialGroup();
    vertical.addContainerGap();

    ParallelGroup firstRow = layout.createParallelGroup();
    firstRow.addComponent(authenticationTypeLabel, Alignment.CENTER);
    firstRow.addComponent(authenticationTypeBox, GroupLayout.PREFERRED_SIZE,
        GroupLayout.DEFAULT_SIZE,
        GroupLayout.PREFERRED_SIZE);
    vertical.addGroup(firstRow);

    vertical.addPreferredGap(ComponentPlacement.UNRELATED);

    if (authenticationTypeBox.getSelectedIndex() == 0) {
      horizontal.addComponent(mojangAuthType);
      vertical.addComponent(mojangAuthType);
    } else {
      horizontal.addComponent(msaAuthType);
      vertical.addComponent(msaAuthType);
      msaSelected();
    }

    vertical.addContainerGap();
    layout.setVerticalGroup(vertical);
    layout.setHorizontalGroup(horizontal);
  }

  private void msaSelected() {
    if (msaAuth != null) {
      return;
    }
    msaAuth = new MsaAuthenticationService(DrainerBot.MSA_CLIENT_ID);
    new Thread(() -> {
      String label;
      try {
        msaResponse = msaAuth.getAuthCode();
        label = msaResponse.message;
      } catch (RequestException e) {
        label = "Unable to get Microsoft information";
      }
      String finalLabel = label;

      SwingUtilities.invokeLater(() -> {
        String replacedLabel = finalLabel;
        String toReplace = "https://www.microsoft.com/link";
        int idx = replacedLabel.indexOf(toReplace);
        if (idx >= 0) {
          replacedLabel =
              replacedLabel.substring(0, idx) + replacedLabel.substring(idx + toReplace.length());
          msaDone.setVisible(true);
        }
        msaPrompt.setText(replacedLabel);
        if (idx >= 0) {
          StyledDocument doc = msaPrompt.getStyledDocument();
          Style regularBlue = doc.addStyle("regularBlue",
              StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE));
          StyleConstants.setForeground(regularBlue, Color.BLUE);
          StyleConstants.setUnderline(regularBlue, true);
          regularBlue.addAttribute("linkact", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
              try {
                Desktop.getDesktop().browse(new URL(toReplace).toURI());
              } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
              }
            }
          });
          try {
            doc.insertString(idx, toReplace, regularBlue);
          } catch (BadLocationException e) {
            e.printStackTrace();
          }
        }
      });
    }).start();
  }

  private void mojangSetup() {
    mojangAuthType = new JPanel();
    GroupLayout layout = new GroupLayout(mojangAuthType);
    mojangAuthType.setLayout(layout);

    ParallelGroup horizontal = layout.createParallelGroup();
    SequentialGroup vertical = layout.createSequentialGroup();

    Box padding = Box.createVerticalBox();
    padding.add(Box.createVerticalGlue());

    Box centredDone = Box.createHorizontalBox();
    centredDone.add(Box.createHorizontalGlue());
    centredDone.add(Box.createRigidArea(new Dimension(1, 1))); // fix off by one pixel
    centredDone.add(mojangDone);
    centredDone.add(Box.createHorizontalGlue());

    horizontal.addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(mojangEmailLabel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(mojangEmailField)
        .addContainerGap());

    horizontal.addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(mojangPasswordLabel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(mojangPasswordField)
        .addContainerGap());

    horizontal.addComponent(padding).addComponent(centredDone, Alignment.CENTER);

    horizontal.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(mojangError)
        .addContainerGap());

    vertical.addGroup(layout.createParallelGroup()
        .addComponent(mojangEmailLabel, Alignment.CENTER)
        .addComponent(mojangEmailField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
            GroupLayout.PREFERRED_SIZE));

    vertical.addPreferredGap(ComponentPlacement.RELATED);

    vertical.addGroup(layout.createParallelGroup()
        .addComponent(mojangPasswordLabel, Alignment.CENTER)
        .addComponent(mojangPasswordField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
            GroupLayout.PREFERRED_SIZE));

    vertical.addGroup(layout.createSequentialGroup().addPreferredGap(ComponentPlacement.UNRELATED)
        .addComponent(mojangError, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
            GroupLayout.PREFERRED_SIZE));

    vertical.addGroup(
        layout.createSequentialGroup().addComponent(padding).addComponent(centredDone));

    layout.linkSize(mojangEmailLabel, mojangPasswordLabel);

    layout.setVerticalGroup(vertical);
    layout.setHorizontalGroup(horizontal);

    mojangAuthType.setMinimumSize(new Dimension(1, 140));

    this.getRootPane().setDefaultButton(mojangDone);
  }

  private void msaSetup() {
    msaAuthType = new JPanel();
    GroupLayout layout = new GroupLayout(msaAuthType);
    msaAuthType.setLayout(layout);

    ParallelGroup horizontal = layout.createParallelGroup();
    SequentialGroup vertical = layout.createSequentialGroup();

    Box padding = Box.createVerticalBox();
    padding.add(Box.createVerticalGlue());

    Box centredMsaDone = Box.createHorizontalBox();
    centredMsaDone.add(Box.createHorizontalGlue());
    centredMsaDone.add(msaDone);
    centredMsaDone.add(Box.createHorizontalGlue());

    horizontal.addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(msaPrompt, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
            GroupLayout.DEFAULT_SIZE).addContainerGap());

    horizontal.addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(msaError).addContainerGap());

    horizontal.addComponent(padding).addComponent(centredMsaDone, Alignment.TRAILING);

    vertical.addGroup(layout.createParallelGroup().addComponent(msaPrompt));
    vertical.addGroup(layout.createParallelGroup().addComponent(msaError));

    vertical.addGroup(
        layout.createSequentialGroup().addComponent(padding).addComponent(centredMsaDone));

    layout.setVerticalGroup(vertical);
    layout.setHorizontalGroup(horizontal);

    msaAuthType.setMinimumSize(new Dimension(1, 140));
  }

  private void mojangClick() {
    if (!mojangDone.isEnabled()) {
      return;
    }
    String text = mojangEmailField.getText();
    if (text.isEmpty()) {
      mojangError.setText("Invalid username");
      return;
    }
    char[] password = mojangPasswordField.getPassword();
    if (password.length == 0) {
      mojangError.setText("Invalid password");
      return;
    }

    mojangDone.setEnabled(false);
    new Thread(() -> {
      MojangAuthenticationService mojang = new MojangAuthenticationService();
      mojang.setUsername(text);
      mojang.setPassword(new String(password));
      try {
        mojang.login();
        new DrainerBot(mojang);
        setVisible(false);
        dispose();
      } catch (RequestException ex) {
        SwingUtilities.invokeLater(() -> {
          if (ex.getMessage().equals("Forbidden")) {
            mojangError.setText("Invalid credentials");
          } else {
            mojangError.setText("Unknown error");
          }
          mojangDone.setEnabled(true);
        });
      }
    }).start();
  }

  private void msaClick() {
    if (!msaDone.isEnabled()) {
      return;
    }
    msaDone.setEnabled(false);
    new Thread(() -> {
      try {
        msaAuth.login();
        new DrainerBot(msaAuth);
        setVisible(false);
        dispose();
      } catch (RequestException ex) {
        SwingUtilities.invokeLater(() -> {
          if (ex instanceof AuthPendingException) {
            msaError.setText("Please input code on website");
          } else {
            msaError.setText("Unknown error");
          }
          msaDone.setEnabled(true);
        });
      }
    }).start();
  }

  public static void main(String[] args) {
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
    try {
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex0) {
      try {
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex1) {
        try {
          UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
        }
      }
    }
    try {
        new DrainerBot();
        return;
    } catch (Exception e) {
      e.printStackTrace();
    }

    SwingUtilities.invokeLater(() -> {
      new LoginGui().setVisible(true);
    });
  }
}