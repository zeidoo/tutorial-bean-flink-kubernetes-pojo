package org.aihistorian.DoFn;

import org.aihistorian.pojos.GenocidePojo;
import org.apache.beam.sdk.transforms.DoFn;

public class PopulateEventDoFn extends DoFn<String, GenocidePojo> {
    @ProcessElement
    public void processElement(ProcessContext c) {
        var input = c.element();

        var genocide = new GenocidePojo();
        genocide.setEvent(input);
        c.output(genocide);
    }
}
