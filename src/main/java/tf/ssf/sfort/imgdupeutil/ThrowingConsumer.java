package tf.ssf.sfort.imgdupeutil;

import java.io.IOException;

public interface ThrowingConsumer{
void accept(String t) throws IOException;
}
