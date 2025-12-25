package tf.ssf.sfort.imgdupeutil;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;

public class MpvManager extends JFrame {
private final JPanel scrollPanel;
public MpvManager() {
	try {
		UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
	} catch (Exception e) {
		e.printStackTrace();
	}
	
	setTitle("MPV Manager");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setLayout(new BorderLayout());
	scrollPanel = new JPanel();
	scrollPanel.setLayout(new BoxLayout(scrollPanel, BoxLayout.Y_AXIS));
	JScrollPane scrollPane = new JScrollPane(scrollPanel);
	scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	
	
	JButton nextButton = new JButton("Next");
	
	JPanel buttonPanel = new JPanel(new FlowLayout());
	buttonPanel.add(nextButton);
	
	add(scrollPane, BorderLayout.CENTER);
	add(buttonPanel, BorderLayout.SOUTH);
	
	nextButton.addActionListener(e -> {
		UnixDomainSocketAddress address=UnixDomainSocketAddress.of(Main.SOCKET_PATH);
		try (SocketChannel test=SocketChannel.open(StandardProtocolFamily.UNIX)){
			test.connect(address);
			Main.sendCommandToMainInstance(test, new String[]{":mpv"});
		}catch (IOException ignored){
		}
	});
	
	setSize(500, 400);
	setLocationRelativeTo(null);
}

public void add(Map.Entry<String, Set<String>> next) {
	scrollPanel.removeAll();
	int i = 0;
	String p;
	JButton fileButton = add(i++ +": "+ (p = next.getKey()), p, null);
	JButton fi=fileButton;
	add(i++ +": "+ (p = Main.getF(next.getKey())), p, ()->fi.setEnabled(false));
	for (String s : next.getValue()) {
		fileButton = add(i++ +": "+ s, s, null);
		JButton fii=fileButton;
		add(i++ +": "+ (p = Main.getF(s)), p, ()->fii.setEnabled(false));
	}
}
public JButton add(String disp, String path, Runnable act) {
	JButton button = new JButton(disp);
	button.addActionListener(e->{
		if (act != null) act.run();
		button.setEnabled(false);
		deleteFile(path);
	});
	scrollPanel.add(button);
	return button;
}
private void deleteFile(String path) {
	File file = new File(path);
	if (file.isDirectory()) {
		deleteDirectory(file);
	} else {
		file.delete();
	}
}

private boolean deleteDirectory(File directory) {
	File[] files = directory.listFiles();
	if (files != null) {
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDirectory(file);
			} else {
				file.delete();
			}
		}
	}
	return directory.delete();
}
}
