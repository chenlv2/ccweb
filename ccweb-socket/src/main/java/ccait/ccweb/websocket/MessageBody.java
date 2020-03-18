package ccait.ccweb.websocket;


public class MessageBody {

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ReceiverInfo getReceiver() {
        return receiver;
    }

    public void setReceiver(ReceiverInfo receiver) {
        this.receiver = receiver;
    }

    public SendMode getSendMode() {
        return sendMode;
    }

    public void setSendMode(SendMode sendMode) {
        this.sendMode = sendMode;
    }

    private String message;
    private ReceiverInfo receiver;
    private SendMode sendMode;
}
