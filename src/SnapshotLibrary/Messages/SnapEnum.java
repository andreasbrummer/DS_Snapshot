package SnapshotLibrary.Messages;

public enum SnapEnum implements SnapMsg {
    MARKER;
    public String getMsgType() {
        return this.name();
    }
}
