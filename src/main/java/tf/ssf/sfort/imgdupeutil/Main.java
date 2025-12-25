package tf.ssf.sfort.imgdupeutil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Main{


public static final Path SOCKET_PATH=Path.of("/tmp/pathstore.sock");
public static final Map<String, Set<String>> store=new HashMap<>();
public static Iterator<Map.Entry<String, Set<String>>> iter=null;
public static int iterI=-1;
public static final List<Process> iterList=new ArrayList<>();
public static MpvManager ui = null;

public static void main(String[] args) throws Exception{
	UnixDomainSocketAddress address=UnixDomainSocketAddress.of(SOCKET_PATH);
	try (SocketChannel test=SocketChannel.open(StandardProtocolFamily.UNIX)){
		test.connect(address);
		sendCommandToMainInstance(test, args);
		return;
	}catch (IOException ignored){
	}
	if (Files.exists(SOCKET_PATH)){
		Files.delete(SOCKET_PATH);
	}
	ServerSocketChannel serverChannel=ServerSocketChannel.open(StandardProtocolFamily.UNIX);
	serverChannel.bind(address);
	System.out.println("Listening...");
	while (true){
		try (SocketChannel client=serverChannel.accept();
			 BufferedReader in=new BufferedReader(
					 new InputStreamReader(Channels.newInputStream(client)))){
			String line=in.readLine();
			if (line == null) continue;
			String[] parts=line.split("\u0000");
			switch (parts.length){
				case 0: printStore(System.out::print);
				case 1:
					if (parts[0].startsWith(":mpvUi")){
						if (ui != null) return;
						ui = new MpvManager();
						ui.setVisible(true);
						break;
					}
					if (parts[0].startsWith(":mpv")){
						if (iter == null){
							iter=store.entrySet().iterator();
							iterI=0;
						}
						iterI++;
						System.out.println("\nplay: "+iterI+"/"+store.size());
						Map.Entry<String, Set<String>> n = iter.next();
						if (ui != null) ui.add(n);
						play(n);
						
						break;
					}
					try (BufferedWriter writer=Files.newBufferedWriter(Paths.get(parts[0]))){
						printStore(writer::write);
						System.out.println("Saved to: "+parts[0]);
					}catch (IOException e){
						System.err.println("Failed to write to file: "+parts[0]);
					}
					break;
				default: handlePaths(parts);
					break;
			}
		}catch (IOException ignored){
		}
	}
}

public static void play(String file){
	try{
		file=Normalizer.normalize(file, Normalizer.Form.NFC);
		int i=iterList.size();
		System.out.println(i+": "+file);
		ProcessBuilder pb=new ProcessBuilder("mpv", "--no-terminal", "--pause", "--osd-msg1="+i+"-"+file, "--", file);
		pb.inheritIO();
		iterList.add(pb.start());
	}catch (Exception e){
		e.printStackTrace();
	}
}

public static void play(Map.Entry<String, Set<String>> entry){
	for (Process p : iterList) p.destroy();
	iterList.clear();
	play(entry.getKey());
	play(getF(entry.getKey()));
	for (String s : entry.getValue()){
		play(s);
		play(getF(s));
	}
}

public static void handlePaths(String... paths){
	if (paths == null || paths.length < 2){
		return;
	}
	
	Set<String> set=new HashSet<>();
	String top=getF(paths[0]);
	for (int i=0; i < paths.length; i++){
		if (!getF(paths[i]).equals(top)){
			set.add(paths[i]);
		}
	}
	
	if (set.size() > 0){
		Set<String> existingSet=store.get(paths[0]);
		if (existingSet == null){
			store.put(paths[0], existingSet=new HashSet<>());
		}
		existingSet.addAll(set);
	}
	
	System.out.print("\rStored: "+store.size());
}

public static String getF(String path){
	int lastSlash=path.lastIndexOf('/');
	return lastSlash >= 0 ? path.substring(0, lastSlash+1) : "";
}

public static void printStore(ThrowingConsumer out) throws IOException{
	synchronized (store){
		if (store.isEmpty()){
			out.accept("Map is empty.");
			return;
		}
		for (Map.Entry<String, Set<String>> entry : store.entrySet().stream().sorted(Map.Entry.<String, Set<String>>comparingByValue(Comparator.comparingInt(Set::size)).reversed()).collect(Collectors.toList())){
			String key=entry.getKey();
			out.accept(key+" ->\n");
			if (entry.getValue().size() == 1){
				out.accept("\t"+entry.getValue().iterator().next()+"\n");
				continue;
			}
			HashMap<String, String> valueMap=new HashMap<>();
			for (String path : entry.getValue()){
				int lastSlash=path.lastIndexOf('/');
				String valueKey=getF(lastSlash >= 0 ? path.substring(0, lastSlash+1) : "");
				String va=valueMap.get(valueKey);
				if (va == null) va="\t";
				else va+=", ";
				valueMap.put(valueKey, va+path.substring(lastSlash+1));
			}
			for (Map.Entry<String, String> e : valueMap.entrySet()){
				out.accept("\t"+e.getKey()+"> "+e.getValue()+"\n");
			}
			out.accept("\n");
		}
	}
}

public static void sendCommandToMainInstance(SocketChannel channel, String[] args){
	PrintWriter out=new PrintWriter(Channels.newOutputStream(channel), true);
	
	if (args.length == 0){
		out.println("");
	}else{
		out.println(String.join("\u0000", args));
	}
	
}
}