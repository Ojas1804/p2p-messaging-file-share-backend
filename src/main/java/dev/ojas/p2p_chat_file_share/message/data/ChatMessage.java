package dev.ojas.p2p_chat_file_share.message.data;

public class ChatMessage extends BaseMessage {
    private String msgId;
    private String encryptedPayload;
    private String signature;

    public ChatMessage() {
        super(MessageType.CHAT_MESSAGE);
    }

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }

    public String getEncryptedPayload() { return encryptedPayload; }
    public void setEncryptedPayload(String encryptedPayload) { this.encryptedPayload = encryptedPayload; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
