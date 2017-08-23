import java.util.Date;

/*
 * Minimalist implementation of the TimeSpan class.
 */
public class TimeSpan {
    private long firstMessage, lastMessage;
    private int messageCounterTerrestrial;
    private int missingSignals;
    private int vsiMessageCounter;
    private int averageSignalStrength;

    public int getMissingSignals() {
        return missingSignals;
    }

    public void setMissingSignals(int missingSignals) {
        this.missingSignals = missingSignals;
    }

    public int getMessageCounterTerrestrial() {
        return messageCounterTerrestrial;
    }

    public void setMessageCounterTerrestrial(int messageCounterTerrestrial) {
        this.messageCounterTerrestrial = messageCounterTerrestrial;
    }

    public TimeSpan(Date firstMessage) {
        this.firstMessage = firstMessage.getTime();
        this.lastMessage = firstMessage.getTime();
    }

    public Date getFirstMessage() {
        return new Date(firstMessage);
    }

    public Date getLastMessage() {
        return new Date(lastMessage);
    }

    public void setLastMessage(Date lastMessage) {
        this.lastMessage = lastMessage.getTime();
    }

    public int getVsiMessageCounter() {
        return vsiMessageCounter;
    }

    public void setVsiMessageCounter(int vsiMessageCounter) {
        this.vsiMessageCounter = vsiMessageCounter;
    }

    public int getAverageSignalStrength() {
        return averageSignalStrength;
    }

    public void setAverageSignalStrength(int averageSignalStrength) {
        this.averageSignalStrength = averageSignalStrength;
    }
}