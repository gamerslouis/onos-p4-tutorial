package me.louisif.pipelines.simpleswitch.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;

import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;

import static me.louisif.pipelines.simpleswitch.PipeconfLoader.APP_NAME;

@Service
@Command(scope = "onos", name = "add-pi-flow-rule")
public class AddPiFlowRule  extends AbstractShellCommand {
    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Argument(index = 1, name = "inPortNumber", description = "In Port Number",
            required = false, multiValued = false)
    String inPortNumberStr = null;

    @Argument(index = 2, name = "outPortNumber", description = "Out Port Number",
            required = false, multiValued = false)
    String outPortNumberStr = null;

    @Option(name = "-d",
            description = "Delete flow rule",
            required = false, multiValued = false)
    private boolean delete = false;

    @Override
    protected void doExecute() {
        FlowRuleService flowRuleService = get(FlowRuleService.class);
        CoreService coreService = get(CoreService.class);

        DeviceId deviceId = DeviceId.deviceId(uri);
        PortNumber inPortNumber = PortNumber.portNumber(inPortNumberStr);
        PortNumber outPortNumber = PortNumber.portNumber(outPortNumberStr);

        final PiCriterion.Builder criterionBuilder = PiCriterion.builder()
                .matchTernary(PiMatchFieldId.of("standard_metadata.ingress_port"), inPortNumber.toLong(), 0x1ff);
        final PiAction piAction = PiAction.builder().withId(PiActionId.of("MyIngress.table0_control.send"))
                .withParameter(new PiActionParam(PiActionParamId.of("port"), outPortNumber.toLong()))
                .build();
        final FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(coreService.getAppId(APP_NAME))
                .forDevice(deviceId)
                .forTable(PiTableId.of("MyIngress.table0_control.table0")).makePermanent().withPriority(65535)
                .withSelector(DefaultTrafficSelector.builder().matchPi(criterionBuilder.build()).build())
                .withTreatment(DefaultTrafficTreatment.builder().piTableAction(piAction).build()).build();

        if (delete) {
            flowRuleService.removeFlowRules(flowRule);
        } else {
            flowRuleService.applyFlowRules(flowRule);
        }
    }
}
