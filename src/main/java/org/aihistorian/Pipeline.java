package org.aihistorian;

import org.aihistorian.DoFn.PopulateEventDoFn;
import org.aihistorian.DoFn.PrintDoFn;
import org.aihistorian.DoFn.SuperComplexAiBasedHistorianDoFn;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.ParDo;

public class Pipeline {
    public static void main(String[] args) {
//        var filePath = "./genocides.txt";
       var filePath = "file:///opt/flink/usrlib/genocides.txt";

       System.out.println("=====  starting pipeline");
        var options = PipelineOptionsFactory.fromArgs(args).withValidation().create();
        var p = org.apache.beam.sdk.Pipeline.create(options);
        p.apply(TextIO.read().from(filePath))
                .apply(ParDo.of(new PopulateEventDoFn()))
                .apply(ParDo.of(new SuperComplexAiBasedHistorianDoFn()))
                .apply(ParDo.of(new PrintDoFn()))
        ;

        p.run().waitUntilFinish();
        System.out.println("=====  pipeline ended");

        try {
            Thread.sleep(10 * 60 * 1000L); // sleep 10 mins
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
