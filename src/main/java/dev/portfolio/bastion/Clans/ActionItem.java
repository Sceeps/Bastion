package dev.portfolio.bastion.Clans;

public class ActionItem {
    private final String type;
    private final String id;

    public ActionItem(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }
}
