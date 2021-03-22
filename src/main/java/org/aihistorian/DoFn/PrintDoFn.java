package org.aihistorian.DoFn;

import org.aihistorian.pojos.GenocidePojo;
import org.apache.beam.sdk.transforms.DoFn;

import java.time.Instant;
import java.time.ZoneOffset;

public class PrintDoFn extends DoFn<GenocidePojo, GenocidePojo> {
    @ProcessElement
    public void processElement(ProcessContext c) {
        var input = c.element();

        var start = Instant
                .ofEpochSecond(input.getWhen().getStartDate().getSeconds(), input.getWhen().getStartDate().getNanos())
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        var out = input.getEvent() + " started in " + start.getYear();
        if (input.getWhen().getIsOngoing()) {
            out += " and is presently ongoing.";
        } else {
            var end = Instant
                    .ofEpochSecond(input.getWhen().getEndDate().getSeconds(), input.getWhen().getStartDate().getNanos())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();

            out += " and ended in " + end.getYear() + ".";
        }

        out += " Perpetrated by " + input.getPerpetrators() + ".";
        System.out.println(out);
    }
}
