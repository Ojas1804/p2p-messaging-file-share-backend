package dev.ojas.p2p_chat_file_share.node.service;

import dev.ojas.p2p_chat_file_share.config.StorageProperties;
import dev.ojas.p2p_chat_file_share.exception.StorageDirNullException;
import dev.ojas.p2p_chat_file_share.node.data.Node;
import dev.ojas.p2p_chat_file_share.node.data.Peer;
import dev.ojas.p2p_chat_file_share.utils.persist.PersistenceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class NodeService {

    PersistenceManager persistenceManager;
    StorageProperties storageProperties;

    @Autowired
    public NodeService(PersistenceManager persistenceManager, StorageProperties storageProperties) {
        this.persistenceManager = persistenceManager;
        this.storageProperties = storageProperties;
    }

    public Peer[] getKnownPeers() throws StorageDirNullException {
        String storageDir = storageProperties.getDir();
        if(storageDir == null) throw new StorageDirNullException("Storage directory not provided. Application side issue.");
        Path storageDirPath = Paths.get(storageDir);
        Node node = persistenceManager.loadWallet(storageDirPath);
        return node.getPeers();
    }
}
