package org.aihistorian.pojos;

import java.io.Serializable;

public class GenocidePojo implements Serializable {
    private String event;
    private String perpetrators;
    private WhenOuterClass.When when;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getPerpetrators() {
        return perpetrators;
    }

    public void setPerpetrators(String perpetrators) {
        this.perpetrators = perpetrators;
    }

    public WhenOuterClass.When getWhen() {
        return when;
    }

    public void setWhen(WhenOuterClass.When when) {
        this.when = when;
    }
}
