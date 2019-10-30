package org.jlab.myquery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import com.mysql.jdbc.NotImplemented;
import jdk.jshell.spi.ExecutionControl;
import org.jlab.mya.*;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.params.*;
import org.jlab.mya.service.IntervalService;
import org.jlab.mya.service.SourceSamplingService;
import org.jlab.mya.stream.IntEventStream;
import org.jlab.mya.stream.wrapped.FloatGraphicalEventBinSampleStream;
import org.jlab.mya.stream.wrapped.FloatIntegrationStream;
import org.jlab.mya.stream.wrapped.FloatSimpleEventBinSampleStream;
import org.jlab.mya.stream.wrapped.LabeledEnumStream;

/**
 *
 * @author adamc, ryans
 */
public class IntervalWebService extends QueryWebService {

    private final IntervalService service;
    private final DataNexus nexus;

    public IntervalWebService(String deployment) {
        nexus = getNexus(deployment);
        service = new IntervalService(nexus);
    }

    public Metadata findMetadata(String c) throws SQLException {
        return service.findMetadata(c);
    }

    public EventStream openEventStream(Metadata metadata, boolean updatesOnly, Instant begin, Instant end,
            boolean enumsAsStrings) throws Exception {
        IntervalQueryParams params = new IntervalQueryParams(metadata, updatesOnly, begin, end);
        EventStream stream = service.openEventStream(params);

        if (enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            stream = new LabeledEnumStream((IntEventStream) stream, extraInfoList);
        }

        return stream;
    }

    public Long count(Metadata metadata, boolean updatesOnly, Instant begin, Instant end) throws SQLException {
        IntervalQueryParams params = new IntervalQueryParams(metadata, updatesOnly, begin, end);
        return service.count(params);
    }


    public EventStream openSampleEventStream(String sampleType, Metadata metadata, Instant begin, Instant end, long limit,
            long count, boolean enumsAsStrings, boolean integrate) throws SQLException, UnsupportedOperationException {

        // TODO: what about String or other non-numeric types?
        EventStream stream;
        SourceSamplingService sourceSampler = new SourceSamplingService(nexus);
        IntervalService intervalService = new IntervalService(nexus);

        if (sampleType == null || sampleType.isEmpty()){
            throw new IllegalArgumentException("sampleType required.  Options include graphical, event, binned");
        }

        EventStream<FloatEvent> innerStream;

        switch(sampleType) {
            case "graphical":
                innerStream = intervalService.openFloatStream(new IntervalQueryParams(metadata, begin, end));

                if(integrate) { // Careful, we have two inner streams now
                    innerStream = new FloatIntegrationStream(innerStream);
                }

                stream = new FloatGraphicalEventBinSampleStream(innerStream, new GraphicalEventBinSamplerParams(limit, count));
                break;
            case "eventsimple": // This is likely never a good sampler option given above...
                innerStream = intervalService.openFloatStream(new IntervalQueryParams(metadata, begin, end));

                if(integrate) { // Careful, we have two inner streams now
                    innerStream = new FloatIntegrationStream(innerStream);
                }

                stream = new FloatSimpleEventBinSampleStream(innerStream, new SimpleEventBinSamplerParams(limit, count));
                break;
            case "myget": // Fastest?  Worst graphical fidelity?

                if(integrate) {
                    throw new UnsupportedOperationException("Integration of input into myget sampler algorithm has not been implemented");
                }

                stream = sourceSampler.openMyGetSampleFloatStream(new MyGetSampleParams(metadata, begin, end, limit));
                break;
            case "mysampler": // Is this one ever a good idea?  Results in n-queries against database.

                if(integrate) {
                    throw new UnsupportedOperationException("Integration of input into mysampler algorithm has not been implemented");
                }

                long stepMillis = ((end.getEpochSecond() - begin.getEpochSecond()) / count) * 1000;  // TODO: Is this right?
                stream = sourceSampler.openMySamplerFloatStream(new MySamplerParams(metadata, begin, stepMillis, count));
                break;
            default:
                throw new IllegalArgumentException("Unrecognized sampleType - " + sampleType + ".  Options include graphical, eventsimple, myget, mysampler");
        }

        if (enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            stream = new LabeledEnumStream((IntEventStream) stream, extraInfoList);
        }

        return stream;
    }
}
