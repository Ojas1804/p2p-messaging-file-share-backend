package dev.ojas.p2p_chat_file_share.node.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node {
    Identity identity;
    Metadata metadata;
    Indices indices;
    Peer[] peers;
    Room[] rooms;
    PendingTransfer[] pendingTransfers;
    Config config;
}// Send file from 2-3 different peers if the file/message is very important and confidential
/* **
{
  "identity": {
    "name": "someting",
    "nodeId": "ab12cd34...",
    "masterXPub": "xpub661A...",
    "masterSeedEnc": {
      "kdf": "PBKDF2",
      "salt": "BASE64(...)",
      "iterations": 200000,
      "cipher": "AES-GCM",
      "ct": "BASE64(...)"
    }
  },
  "indices": {
    "chat": 15,
    "room": 3,
    "file": 27,
    "ephemeral": 102
  },                                       // Indices are used to generate new key by doing indices+1. It will help in generating new key in the HD Wallet.
  "rooms": [
    {
      "roomId": "f4a3...",
      "roomIndex": 3,
      "encryptedRoomKey": "BASE64(...)",
      "createdAt": 1714234123
    }
  ],
  "knownPeers": [
    {"nodeId":"peer1","ip":"1.2.3.4","port":9000,"lastSeen":1714230000}
  ],
  "pendingFileTransfers": [
    {
      "fileId": "d2f3...",
      "fileIndex": 27,
      "fileName": "bigvideo.mp4",
      "fileSize": 123456789,
      "chunkSize": 1048576,
      "totalChunks": 118,
      "receivedChunks": [0,1,2,7,8],
      "encryptedFileKey": "BASE64(...)"
    }
  ],
  "config": {
    "maxChunkSize": 1048576,
    "port": 9000
  }
}

** */