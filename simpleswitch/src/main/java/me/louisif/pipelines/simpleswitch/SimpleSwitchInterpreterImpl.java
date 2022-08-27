package me.louisif.pipelines.simpleswitch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.net.pi.runtime.PiPacketOperation;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static me.louisif.pipelines.simpleswitch.SimpleswitchConstants.*;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.pi.model.PiPacketOperationType.PACKET_OUT;

public class SimpleSwitchInterpreterImpl extends AbstractHandlerBehaviour implements PiPipelineInterpreter {
    private static final int PORT_BITWIDTH = 9;

    private static final Map<Integer, PiTableId> TABLE_MAP = new ImmutableMap.Builder<Integer, PiTableId>()
            .put(0, MY_INGRESS_TABLE0_CONTROL_TABLE0)
            .build();

    private static final Map<Criterion.Type, PiMatchFieldId> CRITERION_MAP =
            new ImmutableMap.Builder<Criterion.Type, PiMatchFieldId>()
                    .put(Criterion.Type.IN_PORT, HDR_STANDARD_METADATA_INGRESS_PORT)
                    .put(Criterion.Type.ETH_SRC, HDR_HDR_ETHERNET_SRC_ADDR)
                    .put(Criterion.Type.ETH_DST, HDR_HDR_ETHERNET_DST_ADDR)
                    .put(Criterion.Type.ETH_TYPE, HDR_HDR_ETHERNET_ETHER_TYPE)
                    .build();

    @Override
    public Optional<PiMatchFieldId> mapCriterionType(Criterion.Type type) {
        return Optional.ofNullable(CRITERION_MAP.get(type));
    }

    @Override
    public Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId) {
        return Optional.ofNullable(TABLE_MAP.get(flowRuleTableId));
    }

    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId) throws PiInterpreterException {
        if (!piTableId.equals(MY_INGRESS_TABLE0_CONTROL_TABLE0)) {
            throw new PiInterpreterException("Unsupported table id: " + piTableId);
        }
        if (treatment.allInstructions().size() != 1 ||
                !treatment.allInstructions().get(0).type().equals(Instruction.Type.OUTPUT)) {
            throw new PiInterpreterException("Only output instruction is supported");
        }
        PortNumber port = ((Instructions.OutputInstruction) treatment.allInstructions().get(0)).port();
        if (port.isLogical()) {
            if (port.exactlyEquals(PortNumber.CONTROLLER)) {
                return PiAction.builder().withId(MY_INGRESS_TABLE0_CONTROL_SEND_TO_CPU)
                        .build();
            } else {
                throw new PiInterpreterException("Unsupported logical port: " + port);
            }
        }
        return PiAction.builder().withId(MY_INGRESS_TABLE0_CONTROL_SEND)
                .withParameter(new PiActionParam(PORT, port.toLong()))
                .build();
    }

    @Override
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet) throws PiInterpreterException {
        TrafficTreatment treatment = packet.treatment();

        // basic.p4 supports only OUTPUT instructions.
        List<Instructions.OutputInstruction> outInstructions = treatment
                .allInstructions()
                .stream()
                .filter(i -> i.type().equals(Instruction.Type.OUTPUT))
                .map(i -> (Instructions.OutputInstruction) i)
                .collect(toList());

        if (treatment.allInstructions().size() != outInstructions.size()) {
            // There are other instructions that are not of type OUTPUT.
            throw new PiInterpreterException("Treatment not supported: " + treatment);
        }

        ImmutableList.Builder<PiPacketOperation> builder = ImmutableList.builder();
        for (Instructions.OutputInstruction outInst : outInstructions) {
            if (outInst.port().isLogical() && !outInst.port().equals(FLOOD)) {
                throw new PiInterpreterException(format(
                        "Output on logical port '%s' not supported", outInst.port()));
            } else if (outInst.port().equals(FLOOD)) {
                // Since basic.p4 does not support flooding, we create a packet
                // operation for each switch port.
                final DeviceService deviceService = handler().get(DeviceService.class);
                for (Port port : deviceService.getPorts(packet.sendThrough())) {
                    builder.add(createPiPacketOperation(packet.data(), port.number().toLong()));
                }
            } else {
                builder.add(createPiPacketOperation(packet.data(), outInst.port().toLong()));
            }
        }
        return builder.build();
    }

    private PiPacketOperation createPiPacketOperation(ByteBuffer data, long portNumber)
            throws PiInterpreterException {
        PiPacketMetadata metadata = createPacketMetadata(portNumber);
        return PiPacketOperation.builder()
                .withType(PACKET_OUT)
                .withData(copyFrom(data))
                .withMetadatas(ImmutableList.of(metadata))
                .build();
    }

    private PiPacketMetadata createPacketMetadata(long portNumber) throws PiInterpreterException {
        try {
            return PiPacketMetadata.builder()
                    .withId(EGRESS_PORT)
                    .withValue(copyFrom(portNumber).fit(PORT_BITWIDTH))
                    .build();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new PiInterpreterException(format(
                    "Port number %d too big, %s", portNumber, e.getMessage()));
        }
    }

    @Override
    public InboundPacket mapInboundPacket(PiPacketOperation packetIn, DeviceId deviceId) throws PiInterpreterException {
        Ethernet ethPkt;
        try {
            ethPkt = Ethernet.deserializer().deserialize(packetIn.data().asArray(), 0,
                                                         packetIn.data().size());
        } catch (DeserializationException dex) {
            throw new PiInterpreterException(dex.getMessage());
        }

        // Returns the ingress port packet metadata.
        Optional<PiPacketMetadata> packetMetadata = packetIn.metadatas()
                .stream().filter(m -> m.id().equals(INGRESS_PORT))
                .findFirst();

        if (packetMetadata.isPresent()) {
            ImmutableByteSequence portByteSequence = packetMetadata.get().value();
            short s = portByteSequence.asReadOnlyBuffer().getShort();
            ConnectPoint receivedFrom = new ConnectPoint(deviceId, PortNumber.portNumber(s));
            ByteBuffer rawData = ByteBuffer.wrap(packetIn.data().asArray());
            return new DefaultInboundPacket(receivedFrom, ethPkt, rawData);
        } else {
            throw new PiInterpreterException(format(
                    "Missing metadata '%s' in packet-in received from '%s': %s",
                    INGRESS_PORT, deviceId, packetIn));
        }
    }

//    @Override
//    public Optional<Integer> mapLogicalPortNumber(PortNumber port) {
//        return PiPipelineInterpreter.super.mapLogicalPortNumber(port);
//    }
//
//    @Override
//    public Optional<PiAction> getOriginalDefaultAction(PiTableId tableId) {
//        return PiPipelineInterpreter.super.getOriginalDefaultAction(tableId);
//    }
}
