package org.aihistorian.DoFn;

import com.google.protobuf.util.Timestamps;
import org.aihistorian.pojos.GenocidePojo;
import org.aihistorian.pojos.WhenOuterClass;
import org.apache.beam.sdk.transforms.DoFn;

import java.time.LocalDate;
import java.time.ZoneOffset;

public class SuperComplexAiBasedHistorianDoFn extends DoFn<GenocidePojo, GenocidePojo> {
    @ProcessElement
    public void processElement(ProcessContext c) {
        var input = c.element();

        if (input.getEvent().equals("Rohingya genocide")) {
            input.setPerpetrators("Myanmar's armed forces and police");
            setWhen(input, 2016, 0, true);
        }
        if (input.getEvent().equals("Darfur genocide")) {
            input.setPerpetrators("Khartoum government, Janjaweed, the Justice and Equality Movement and the Sudan Liberation Army");
            setWhen(input, 2003, 0, true);
        }
        if (input.getEvent().equals("The Holocaust")) {
            input.setPerpetrators("Nazi Germany");
            setWhen(input, 1941, 1945, false);
        }
        if (input.getEvent().equals("Armenian genocide")) {
            input.setPerpetrators("Ottoman Empire/Turkey");
            setWhen(input, 1915, 1922, false);
        }
        if (input.getEvent().equals("Cambodian genocide")) {
            input.setPerpetrators("Khmer Rouge");
            setWhen(input, 1975, 1979, false);
        }
        if (input.getEvent().equals("East Timor genocide")) {
            input.setPerpetrators("Indonesian New Order government");
            setWhen(input, 1975, 1999, false);
        }
        if (input.getEvent().equals("Uyghur genocide")) {
            input.setPerpetrators("People's Republic of China / Chinese Communist Party");
            setWhen(input, 2014, 0, true);
        }

        c.output(input);
    }

    private void setWhen(GenocidePojo input, int startYear, int endYear, boolean isOngoing) {
        var start = LocalDate.of(startYear, 1, 1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        var when = WhenOuterClass.When.newBuilder()
                .setStartDate(Timestamps.fromMillis(start.toEpochMilli()))
                .setIsOngoing(isOngoing);
        if (endYear != 0) {
            var end = LocalDate.of(endYear, 1, 1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            when.setEndDate(Timestamps.fromMillis(end.toEpochMilli()));
        }

        input.setWhen(when.build());
    }
}
