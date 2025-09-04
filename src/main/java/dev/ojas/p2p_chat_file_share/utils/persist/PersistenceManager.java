package dev.ojas.p2p_chat_file_share.utils.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ojas.p2p_chat_file_share.config.StorageProperties;
import dev.ojas.p2p_chat_file_share.node.data.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;

@Service
public class PersistenceManager {
    @Autowired
    private static StorageProperties storageProperties;
    private static final ObjectMapper M = new ObjectMapper();
    private static final String userDetailsFileName = storageProperties.getUserDetailsFileName();


    public void saveWallet(Node node, Path dir) {
        try {
            if (!dir.toFile().exists()) dir.toFile().mkdirs();
            File f = dir.resolve(userDetailsFileName).toFile();
            M.writerWithDefaultPrettyPrinter().writeValue(f, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Node loadWallet(Path dir) {
        try {
            File f = dir.resolve(userDetailsFileName).toFile();
            if (!f.exists()) return null;
            return M.readValue(f, Node.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
