package me.louisif.pipelines.simpleswitch;

import org.onosproject.core.CoreService;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.model.P4InfoParser;
import org.onosproject.p4runtime.model.P4InfoParserException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.net.URL;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.*;

@Component(enabled = true)
public final class PipeconfLoader {
    public static final String APP_NAME = "me.louisif.pipelines.simpleswitch";
    private static final PiPipeconfId PIPECONF_ID = new PiPipeconfId(APP_NAME);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfService piPipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Activate
    public void activate() throws P4InfoParserException {
        coreService.registerApplication(APP_NAME);

        final URL jsonUrl = PipeconfLoader.class.getResource("/p4c-out/bmv2/simpleswitch.json");
        final URL p4InfoUrl = PipeconfLoader.class.getResource("/p4c-out/bmv2/simpleswitch_p4info.txt");

        PiPipeconf pipeconf = DefaultPiPipeconf.builder()
                .withId(PIPECONF_ID)
                .withPipelineModel(P4InfoParser.parse(p4InfoUrl))
                .addBehaviour(PiPipelineInterpreter.class, SimpleSwitchInterpreterImpl.class)
                .addBehaviour(Pipeliner.class, SimpleSwitchPipeliner.class)
                .addExtension(P4_INFO_TEXT, p4InfoUrl)
                .addExtension(BMV2_JSON, jsonUrl)
                .build();
        piPipeconfService.register(pipeconf);
    }

    @Deactivate
    public void deactivate() {
        piPipeconfService.unregister(PIPECONF_ID);
    }
}
