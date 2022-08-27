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

import static me.louisif.pipelines.simpleswitch.PipeconfLoader.APP_NAME;

@Service
@Command(scope = "onos", name = "add-common-flow-rule")
public class AddFlowRule  extends AbstractShellCommand {
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

        final FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(coreService.getAppId(APP_NAME))
                .forDevice(deviceId)
                .forTable(0).makePermanent().withPriority(65535)
                .withSelector(DefaultTrafficSelector.builder().matchInPort(inPortNumber).build())
                .withTreatment(DefaultTrafficTreatment.builder().setOutput(outPortNumber).build()).build();

        if (delete) {
            flowRuleService.removeFlowRules(flowRule);
        } else {
            flowRuleService.applyFlowRules(flowRule);
        }
    }
}
